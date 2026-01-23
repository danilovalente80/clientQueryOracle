package com.oracle.client.service;

import com.oracle.client.config.DatabaseAlias;
import com.oracle.client.model.QueryRequest;
import com.oracle.client.model.QueryResponse;
import com.oracle.client.util.QueryParser;
import com.oracle.client.util.QuerySplitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateful EJB for executing Oracle queries with manual transaction management.
 * Uses Bean Managed Transactions (BMT) with UserTransaction to allow explicit commit/rollback control
 * across multiple method invocations.
 * Supports executing queries on multiple databases (aliases) within the same transaction.
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class QueryExecutorServiceBean implements QueryExecutorService {

    private static final Logger LOGGER = Logger.getLogger(QueryExecutorServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @EJB
    private QueryLogService queryLogService;

    // Support multiple connections for different databases in the same transaction
    private Map<String, Connection> connections = new HashMap<String, Connection>();
    private List<String> usedAliases = new ArrayList<String>();
    private int lastAffectedRows;
    private String sessionId;
    private UserTransaction userTransaction;
    private boolean transactionActive = false;

    // For logging
    private String currentUsername;
    private String lastQuery;

    @PostConstruct
    public void init() {
        sessionId = UUID.randomUUID().toString();
        LOGGER.info("QueryExecutorService initialized with session ID: " + sessionId);
    }

    @Override
    public QueryResponse executeQuery(QueryRequest request) {
        try {
            // Validate request
            if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return QueryResponse.error("Query cannot be null or empty", null);
            }

            String query = request.getQuery().trim();

            // Validate query type (only DML allowed: INSERT, UPDATE, DELETE)
            String queryUpperCase = query.toUpperCase();
            if (!queryUpperCase.startsWith("INSERT") &&
                !queryUpperCase.startsWith("UPDATE") &&
                !queryUpperCase.startsWith("DELETE")) {
                return QueryResponse.error(
                    "Only INSERT, UPDATE, and DELETE queries are allowed",
                    "Query type not supported. SELECT and DDL statements are not permitted."
                );
            }

            // Begin UserTransaction if not already active
            if (!transactionActive) {
                userTransaction = sessionContext.getUserTransaction();
                userTransaction.begin();
                transactionActive = true;
                LOGGER.info("UserTransaction started for session: " + sessionId);
            }

            // Store for logging
            currentUsername = request.getUsername();
            lastQuery = query;
            long startTime = System.currentTimeMillis();

            // Split queries (support multiple queries separated by semicolon)
            List<String> queries = QuerySplitter.splitQueries(query);
            LOGGER.info("Found " + queries.size() + " query(ies) to execute");

            List<Integer> affectedRowsList = new ArrayList<Integer>();
            int totalAffectedRows = 0;

            // Execute each query - detect alias for each individual query
            for (int i = 0; i < queries.size(); i++) {
                String singleQuery = queries.get(i);
                PreparedStatement statement = null;

                try {
                    // Determine database alias for this specific query
                    String aliasToUse = null;

                    // First, check if alias was explicitly provided
                    if (request.getAlias() != null && !request.getAlias().trim().isEmpty()) {
                        aliasToUse = request.getAlias().trim();
                        LOGGER.info("Using provided alias: " + aliasToUse);
                    } else {
                        // Try to extract alias from this specific query
                        String extractedAlias = QueryParser.extractAlias(singleQuery);
                        if (extractedAlias != null) {
                            aliasToUse = extractedAlias;
                            LOGGER.info("Extracted alias from query " + (i + 1) + ": " + aliasToUse);
                        } else {
                            // Rollback and return error
                            if (transactionActive) {
                                rollbackUserTransaction();
                            }
                            return QueryResponse.error(
                                "Cannot determine database alias for query " + (i + 1),
                                "Please either:\n" +
                                "1. Select a database from the dropdown, OR\n" +
                                "2. Use schema-qualified table names in your query (e.g., sesamo.uffici)\n\n" +
                                "Available aliases: " + DatabaseAlias.getAvailableAliases()
                            );
                        }
                    }

                    // Get database alias and JNDI name
                    DatabaseAlias dbAlias;
                    try {
                        dbAlias = DatabaseAlias.fromAlias(aliasToUse);
                    } catch (IllegalArgumentException e) {
                        // Rollback and return error
                        if (transactionActive) {
                            rollbackUserTransaction();
                        }
                        return QueryResponse.error("Invalid database alias: " + aliasToUse, e.getMessage());
                    }

                    // Get or create connection for this alias
                    Connection conn = getOrCreateConnection(dbAlias);

                    LOGGER.info("Executing query " + (i + 1) + "/" + queries.size() + " on " +
                        dbAlias.getAlias() + ": " + singleQuery.substring(0, Math.min(100, singleQuery.length())));

                    statement = conn.prepareStatement(singleQuery);
                    int affected = statement.executeUpdate();
                    affectedRowsList.add(affected);
                    totalAffectedRows += affected;

                    LOGGER.info("Query " + (i + 1) + " executed successfully on " +
                        dbAlias.getAlias() + ". Affected rows: " + affected);

                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Error closing statement", e);
                        }
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            lastAffectedRows = totalAffectedRows;

            // Build affected rows detail string
            StringBuilder detailBuilder = new StringBuilder();
            for (int i = 0; i < affectedRowsList.size(); i++) {
                detailBuilder.append(affectedRowsList.get(i));
                if (i < affectedRowsList.size() - 1) {
                    detailBuilder.append(" - ");
                }
            }
            String affectedRowsDetail = detailBuilder.toString();

            // Log successful query execution (use all aliases involved)
            if (queryLogService != null && currentUsername != null) {
                String aliasesUsed = usedAliases.isEmpty() ? "unknown" : String.join(",", usedAliases);
                queryLogService.logQuerySuccess(currentUsername, aliasesUsed, query,
                    totalAffectedRows, duration, sessionId, request.getIpAddress());
            }

            // Build response
            QueryResponse response = QueryResponse.success(
                totalAffectedRows,
                "Query executed successfully. " + totalAffectedRows + " row(s) affected. " +
                "Transaction pending - please commit or rollback."
            );
            response.setAffectedRowsDetail(affectedRowsDetail);

            return response;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error executing query", e);

            // Log error
            if (queryLogService != null && request.getUsername() != null) {
                String aliasesUsed = usedAliases.isEmpty() ? "unknown" : String.join(",", usedAliases);
                queryLogService.logQueryError(request.getUsername(), aliasesUsed,
                    request.getQuery(), e.getMessage(),
                    "SQLState:" + e.getSQLState() + " Code:" + e.getErrorCode(),
                    0, sessionId, request.getIpAddress());
            }

            // Rollback on SQL error
            if (transactionActive) {
                rollbackUserTransaction();
            }
            return QueryResponse.error(
                "SQL Error: " + e.getMessage(),
                "SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error executing query", e);

            // Log error
            if (queryLogService != null && request.getUsername() != null) {
                String aliasesUsed = usedAliases.isEmpty() ? "unknown" : String.join(",", usedAliases);
                queryLogService.logQueryError(request.getUsername(), aliasesUsed,
                    request.getQuery(), e.getMessage(), e.getClass().getName(),
                    0, sessionId, request.getIpAddress());
            }

            // Rollback on any error
            if (transactionActive) {
                rollbackUserTransaction();
            }
            return QueryResponse.error(
                "Error executing query: " + e.getMessage(),
                e.getClass().getName()
            );
        }
    }

    @Override
    public QueryResponse commitTransaction() {
        if (!transactionActive) {
            return QueryResponse.error("No active transaction to commit", null);
        }

        try {
            userTransaction.commit();
            transactionActive = false;
            String aliasesInfo = usedAliases.isEmpty() ? "unknown" : String.join(",", usedAliases);
            LOGGER.info("UserTransaction committed successfully on databases: " + aliasesInfo +
                ". Rows affected: " + lastAffectedRows);

            // Log commit
            if (queryLogService != null && currentUsername != null) {
                queryLogService.logCommit(currentUsername, aliasesInfo, lastAffectedRows, sessionId);
            }

            // Close all connections after successful commit
            closeAllConnections();

            return QueryResponse.success(
                lastAffectedRows,
                "Transaction committed successfully on database(s): " + aliasesInfo +
                ". " + lastAffectedRows + " row(s) affected."
            );

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error committing transaction", e);
            // Try to rollback after failed commit
            rollbackUserTransaction();
            return QueryResponse.error(
                "Error committing transaction: " + e.getMessage(),
                e.getClass().getName()
            );
        }
    }

    @Override
    public QueryResponse rollbackTransaction() {
        if (!transactionActive) {
            return QueryResponse.error("No active transaction to rollback", null);
        }

        try {
            rollbackUserTransaction();
            String aliasesInfo = usedAliases.isEmpty() ? "unknown" : String.join(",", usedAliases);
            LOGGER.info("UserTransaction rolled back successfully on databases: " + aliasesInfo);

            // Log rollback
            if (queryLogService != null && currentUsername != null) {
                queryLogService.logRollback(currentUsername, aliasesInfo, sessionId);
            }

            // Close all connections after rollback
            closeAllConnections();

            return QueryResponse.success(
                0,
                "Transaction rolled back successfully on database(s): " + aliasesInfo +
                ". Changes have been discarded."
            );

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rolling back transaction", e);
            return QueryResponse.error(
                "Error rolling back transaction: " + e.getMessage(),
                e.getClass().getName()
            );
        }
    }

    @Override
    public boolean hasActiveTransaction() {
        return transactionActive;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get or create connection for the specified database alias.
     * All connections are automatically enlisted in the current UserTransaction.
     */
    private Connection getOrCreateConnection(DatabaseAlias dbAlias) throws Exception {
        String alias = dbAlias.getAlias();

        // Check if connection already exists for this alias
        if (connections.containsKey(alias)) {
            Connection existingConn = connections.get(alias);
            // Verify connection is still valid
            if (existingConn != null && !existingConn.isClosed()) {
                LOGGER.info("Reusing existing connection for alias: " + alias);
                return existingConn;
            } else {
                // Connection is closed, remove it
                connections.remove(alias);
            }
        }

        // Create new connection
        LOGGER.info("Looking up datasource: " + dbAlias.getJndiName());
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(dbAlias.getJndiName());

        Connection conn = ds.getConnection();
        LOGGER.info("New connection established for alias: " + alias);

        // Store connection and track alias usage
        connections.put(alias, conn);
        if (!usedAliases.contains(alias)) {
            usedAliases.add(alias);
        }

        return conn;
    }

    /**
     * Rollback UserTransaction internally.
     */
    private void rollbackUserTransaction() {
        try {
            if (userTransaction != null && transactionActive) {
                userTransaction.rollback();
                transactionActive = false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error rolling back UserTransaction", e);
            transactionActive = false;
        }
    }

    /**
     * Close all database connections.
     */
    private void closeAllConnections() {
        for (Map.Entry<String, Connection> entry : connections.entrySet()) {
            String alias = entry.getKey();
            Connection conn = entry.getValue();
            if (conn != null) {
                try {
                    conn.close();
                    LOGGER.info("Connection closed for alias: " + alias);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing connection for alias: " + alias, e);
                }
            }
        }
        connections.clear();
        usedAliases.clear();
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("QueryExecutorService cleanup - session ID: " + sessionId);

        // Rollback any active transaction before cleanup
        if (transactionActive) {
            LOGGER.warning("Cleaning up with active transaction - rolling back");
            rollbackUserTransaction();
        }

        closeAllConnections();
    }
}
