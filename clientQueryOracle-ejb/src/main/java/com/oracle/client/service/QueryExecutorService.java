package com.oracle.client.service;

import com.oracle.client.model.QueryRequest;
import com.oracle.client.model.QueryResponse;

import javax.ejb.Local;

/**
 * Local interface for Query Executor Service.
 */
@Local
public interface QueryExecutorService {

    /**
     * Execute a query (INSERT, UPDATE, DELETE) and return affected rows.
     * The transaction remains open until commit or rollback is called.
     *
     * @param request QueryRequest containing SQL and alias
     * @return QueryResponse with execution results
     */
    QueryResponse executeQuery(QueryRequest request);

    /**
     * Commit the current transaction.
     *
     * @return QueryResponse indicating commit success or failure
     */
    QueryResponse commitTransaction();

    /**
     * Rollback the current transaction.
     *
     * @return QueryResponse indicating rollback success or failure
     */
    QueryResponse rollbackTransaction();

    /**
     * Check if there is an active transaction.
     *
     * @return true if transaction is active, false otherwise
     */
    boolean hasActiveTransaction();

    /**
     * Get the current session ID for tracking.
     *
     * @return session ID string
     */
    String getSessionId();
}
