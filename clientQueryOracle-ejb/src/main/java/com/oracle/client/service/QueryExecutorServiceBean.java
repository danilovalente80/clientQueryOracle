package com.oracle.client.service;

import com.oracle.client.config.DatabaseAlias;
import com.oracle.client.model.QueryRequest;
import com.oracle.client.model.QueryResponse;
import com.oracle.client.util.QueryParser;

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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateful EJB for executing Oracle queries with manual transaction management.
 * Uses Bean Managed Transactions (BMT) with UserTransaction to allow explicit commit/rollback control
 * across multiple method invocations.
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class QueryExecutorServiceBean implements QueryExecutorService {

    private static final Logger LOGGER = Logger.getLogger(QueryExecutorServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @EJB
    private QueryLogService queryLogService;

    private Connection connection;
    private String currentAlias;
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

            // Determine database alias: use provided alias or extract from query
            String aliasToUse = null;

            // First, check if alias was explicitly provided
            if (request.getAlias() != null && !request.getAlias().trim().isEmpty()) {
                aliasToUse = request.getAlias().trim();
                LOGGER.info("Using provided alias: " + aliasToUse);
            } else {
                // Try to extract alias from query (e.g., from "UPDATE sesamo.uffici SET...")
                String extractedAlias = QueryParser.extractAlias(query);
                if (extractedAlias != null) {
                    aliasToUse = extractedAlias;
                    LOGGER.info("Extracted alias from query: " + aliasToUse + " - " +
                        QueryParser.getAliasDetectionInfo(query));
                } else {
                    return QueryResponse.error(
                        "Cannot determine database alias",
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
                return QueryResponse.error("Invalid database alias: " + aliasToUse, e.getMessage());
            }

            // If alias changed or no connection, establish new connection
            if (connection == null || currentAlias == null || !currentAlias.equals(aliasToUse)) {
                // Close existing transaction and connection if changing database
                if (transactionActive) {
                    LOGGER.warning("Changing database alias - rolling back existing transaction");
                    rollbackUserTransaction();
                }
                closeConnection();
                connection = getConnection(dbAlias);
                currentAlias = aliasToUse;
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

            // Execute query
            PreparedStatement statement = null;
            try {
                LOGGER.info("Executing query on " + dbAlias.getAlias() + ": " +
                    query.substring(0, Math.min(100, query.length())));

                statement = connection.prepareStatement(query);
                lastAffectedRows = statement.executeUpdate();
                long duration = System.currentTimeMillis() - startTime;

                LOGGER.info("Query executed successfully. Affected rows: " + lastAffectedRows);

                // Log successful query execution
                if (queryLogService != null && currentUsername != null) {
                    queryLogService.logQuerySuccess(currentUsername, aliasToUse, query,
                        lastAffectedRows, duration, sessionId, request.getIpAddress());
                }

                return QueryResponse.success(
                    lastAffectedRows,
                    "Query executed successfully. " + lastAffectedRows + " row(s) affected. " +
                    "Transaction pending - please commit or rollback."
                );

            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Error closing statement", e);
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error executing query", e);

            // Log error
            if (queryLogService != null && request.getUsername() != null) {
                queryLogService.logQueryError(request.getUsername(), currentAlias,
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
                queryLogService.logQueryError(request.getUsername(), currentAlias,
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
            LOGGER.info("UserTransaction committed successfully. Rows affected: " + lastAffectedRows);

            // Log commit
            if (queryLogService != null && currentUsername != null) {
                queryLogService.logCommit(currentUsername, currentAlias, lastAffectedRows, sessionId);
            }

            return QueryResponse.success(
                lastAffectedRows,
                "Transaction committed successfully. " + lastAffectedRows + " row(s) affected."
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
            LOGGER.info("UserTransaction rolled back successfully");

            // Log rollback
            if (queryLogService != null && currentUsername != null) {
                queryLogService.logRollback(currentUsername, currentAlias, sessionId);
            }

            return QueryResponse.success(
                0,
                "Transaction rolled back successfully. Changes have been discarded."
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
     * Get database connection via JNDI lookup.
     */
    private Connection getConnection(DatabaseAlias dbAlias) throws Exception {
        LOGGER.info("Looking up datasource: " + dbAlias.getJndiName());

        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(dbAlias.getJndiName());

        Connection conn = ds.getConnection();
        LOGGER.info("Connection established for alias: " + dbAlias.getAlias());

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
     * Close the current connection.
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Connection closed");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
            connection = null;
            currentAlias = null;
        }
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("QueryExecutorService cleanup - session ID: " + sessionId);

        // Rollback any active transaction before cleanup
        if (transactionActive) {
            LOGGER.warning("Cleaning up with active transaction - rolling back");
            rollbackUserTransaction();
        }

        closeConnection();
    }
}
