package com.oracle.client.service;

import javax.ejb.Local;

/**
 * Service for logging query execution to database.
 */
@Local
public interface QueryLogService {

    /**
     * Log successful query execution.
     *
     * @param username Username who executed the query
     * @param databaseAlias Database alias (sesamo, entr_asp, etc.)
     * @param queryText Full SQL query text
     * @param affectedRows Number of rows affected
     * @param durationMs Execution duration in milliseconds
     * @param sessionId Session ID
     * @param ipAddress Client IP address
     */
    void logQuerySuccess(String username, String databaseAlias, String queryText,
                        int affectedRows, long durationMs, String sessionId, String ipAddress);

    /**
     * Log failed query execution.
     *
     * @param username Username who executed the query
     * @param databaseAlias Database alias
     * @param queryText Full SQL query text
     * @param errorMessage Error message
     * @param errorCode SQL error code or exception type
     * @param durationMs Execution duration in milliseconds
     * @param sessionId Session ID
     * @param ipAddress Client IP address
     */
    void logQueryError(String username, String databaseAlias, String queryText,
                      String errorMessage, String errorCode, long durationMs,
                      String sessionId, String ipAddress);

    /**
     * Log transaction commit.
     *
     * @param username Username
     * @param databaseAlias Database alias
     * @param affectedRows Number of rows committed
     * @param sessionId Session ID
     */
    void logCommit(String username, String databaseAlias, int affectedRows, String sessionId);

    /**
     * Log transaction rollback.
     *
     * @param username Username
     * @param databaseAlias Database alias
     * @param sessionId Session ID
     */
    void logRollback(String username, String databaseAlias, String sessionId);
}
