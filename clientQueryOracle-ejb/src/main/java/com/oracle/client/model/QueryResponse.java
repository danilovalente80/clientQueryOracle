package com.oracle.client.model;

import java.io.Serializable;

/**
 * Response object containing query execution results.
 */
public class QueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private int affectedRows;
    private String message;
    private String errorDetails;
    private String affectedRowsDetail;  // For multiple queries: "1 - 2 - 1"

    public QueryResponse() {
    }

    public QueryResponse(boolean success, int affectedRows, String message) {
        this.success = success;
        this.affectedRows = affectedRows;
        this.message = message;
    }

    public static QueryResponse success(int affectedRows, String message) {
        return new QueryResponse(true, affectedRows, message);
    }

    public static QueryResponse error(String message, String errorDetails) {
        QueryResponse response = new QueryResponse(false, 0, message);
        response.setErrorDetails(errorDetails);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getAffectedRowsDetail() {
        return affectedRowsDetail;
    }

    public void setAffectedRowsDetail(String affectedRowsDetail) {
        this.affectedRowsDetail = affectedRowsDetail;
    }

    @Override
    public String toString() {
        return "QueryResponse{" +
                "success=" + success +
                ", affectedRows=" + affectedRows +
                ", message='" + message + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                '}';
    }
}
