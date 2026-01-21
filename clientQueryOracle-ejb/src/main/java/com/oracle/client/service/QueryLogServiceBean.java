package com.oracle.client.service;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless EJB for logging query execution to sesamo.client_log_query table.
 * All logging operations are asynchronous to avoid impacting query performance.
 */
@Stateless
public class QueryLogServiceBean implements QueryLogService {

    private static final Logger LOGGER = Logger.getLogger(QueryLogServiceBean.class.getName());

    // JNDI for SESAMO database (log database)
    private static final String LOG_DB_JNDI = "jdbc/ds_sesamo";

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logQuerySuccess(String username, String databaseAlias, String queryText,
                               int affectedRows, long durationMs, String sessionId, String ipAddress) {
        insertLog(username, databaseAlias, queryText, affectedRows, "SUCCESS",
                 null, null, durationMs, sessionId, ipAddress);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logQueryError(String username, String databaseAlias, String queryText,
                             String errorMessage, String errorCode, long durationMs,
                             String sessionId, String ipAddress) {
        insertLog(username, databaseAlias, queryText, 0, "ERROR",
                 errorMessage, errorCode, durationMs, sessionId, ipAddress);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logCommit(String username, String databaseAlias, int affectedRows, String sessionId) {
        insertLog(username, databaseAlias, "COMMIT TRANSACTION", affectedRows,
                 "COMMIT", null, null, 0, sessionId, null);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logRollback(String username, String databaseAlias, String sessionId) {
        insertLog(username, databaseAlias, "ROLLBACK TRANSACTION", 0,
                 "ROLLBACK", null, null, 0, sessionId, null);
    }

    /**
     * Internal method to insert log record.
     */
    private void insertLog(String username, String databaseAlias, String queryText,
                          int affectedRows, String esito, String errorMessage,
                          String errorCode, long durationMs, String sessionId, String ipAddress) {

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getLogConnection();

            String sql =
                "INSERT INTO sesamo.client_log_query " +
                "(id_log, username, database_alias, query_text, num_record, esito, " +
                " error_message, error_code, data_esecuzione, durata_ms, session_id, ip_address) " +
                "VALUES (sesamo.client_log_query_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, databaseAlias);
            stmt.setString(3, queryText);
            stmt.setInt(4, affectedRows);
            stmt.setString(5, esito);
            stmt.setString(6, truncate(errorMessage, 4000));
            stmt.setString(7, truncate(errorCode, 50));
            stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            stmt.setLong(9, durationMs);
            stmt.setString(10, truncate(sessionId, 100));
            stmt.setString(11, truncate(ipAddress, 50));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                LOGGER.fine("Query log inserted: user=" + username + ", esito=" + esito +
                           ", rows=" + affectedRows);
            }

            // Commit immediately (REQUIRES_NEW transaction)
            conn.commit();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error inserting query log", e);
            // Don't rethrow - logging should not fail the main operation
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                LOGGER.log(Level.WARNING, "Error rolling back log transaction", rollbackEx);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inserting query log", e);
        } finally {
            closeResources(stmt, conn);
        }
    }

    /**
     * Get connection to log database (SESAMO).
     */
    private Connection getLogConnection() throws Exception {
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(LOG_DB_JNDI);
        Connection conn = ds.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    /**
     * Truncate string to maximum length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * Close JDBC resources safely.
     */
    private void closeResources(PreparedStatement stmt, Connection conn) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing Statement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing Connection", e);
            }
        }
    }
}
