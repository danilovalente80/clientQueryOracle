package com.oracle.client.model;

import java.io.Serializable;

/**
 * Request object containing the SQL query and database alias.
 */
public class QueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String query;
    private String alias;
    private String username;  // For logging
    private String ipAddress; // For logging

    public QueryRequest() {
    }

    public QueryRequest(String query, String alias) {
        this.query = query;
        this.alias = alias;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "query='" + (query != null ? query.substring(0, Math.min(50, query.length())) + "..." : "null") + '\'' +
                ", alias='" + alias + '\'' +
                '}';
    }
}
