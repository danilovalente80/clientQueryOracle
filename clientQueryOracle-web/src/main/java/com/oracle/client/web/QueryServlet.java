package com.oracle.client.web;

import com.oracle.client.config.DatabaseAlias;
import com.oracle.client.model.QueryRequest;
import com.oracle.client.model.QueryResponse;
import com.oracle.client.model.User;
import com.oracle.client.service.QueryExecutorService;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for handling query execution requests from the frontend.
 * Provides endpoints for executing queries, committing, and rolling back transactions.
 */
@WebServlet(name = "QueryServlet", urlPatterns = {"/api/query/*"})
public class QueryServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(QueryServlet.class.getName());
    private static final long serialVersionUID = 1L;

    @EJB
    private QueryExecutorService queryExecutorService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            // Handle different endpoints
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/execute")) {
                handleExecuteQuery(request, response, out);
            } else if (pathInfo.equals("/commit")) {
                handleCommit(request, response, out);
            } else if (pathInfo.equals("/rollback")) {
                handleRollback(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"message\":\"Endpoint not found\"}");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Internal server error: " +
                escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo != null && pathInfo.equals("/aliases")) {
                handleGetAliases(response, out);
            } else if (pathInfo != null && pathInfo.equals("/status")) {
                handleGetStatus(response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"message\":\"Endpoint not found\"}");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GET request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Internal server error: " +
                escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handle query execution request.
     */
    private void handleExecuteQuery(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws IOException {

        // Check authentication
        User user = LoginServlet.getAuthenticatedUser(request);
        if (user == null || !user.isValid()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"success\":false,\"message\":\"Not authenticated or session expired\"}");
            return;
        }

        // Ensure we have a stateful EJB for this session
        HttpSession session = request.getSession(true);

        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String requestBody = sb.toString();
        LOGGER.info("Received query request from user: " + user.getUsername());

        // Parse JSON manually (simple parsing for basic JSON)
        QueryRequest queryRequest = parseQueryRequest(requestBody);

        if (queryRequest == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Invalid request format\"}");
            return;
        }

        // Add user info for logging
        queryRequest.setUsername(user.getUsername());
        queryRequest.setIpAddress(getClientIP(request));

        // Execute query
        QueryResponse queryResponse = queryExecutorService.executeQuery(queryRequest);

        // Write response
        response.setStatus(queryResponse.isSuccess() ?
            HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        out.write(toJson(queryResponse));
    }

    /**
     * Handle commit request.
     */
    private void handleCommit(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        // Check authentication
        User user = LoginServlet.getAuthenticatedUser(request);
        if (user == null || !user.isValid()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"success\":false,\"message\":\"Not authenticated or session expired\"}");
            return;
        }

        QueryResponse queryResponse = queryExecutorService.commitTransaction();

        response.setStatus(queryResponse.isSuccess() ?
            HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        out.write(toJson(queryResponse));
    }

    /**
     * Handle rollback request.
     */
    private void handleRollback(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        // Check authentication
        User user = LoginServlet.getAuthenticatedUser(request);
        if (user == null || !user.isValid()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"success\":false,\"message\":\"Not authenticated or session expired\"}");
            return;
        }

        QueryResponse queryResponse = queryExecutorService.rollbackTransaction();

        response.setStatus(queryResponse.isSuccess() ?
            HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        out.write(toJson(queryResponse));
    }

    /**
     * Handle get available aliases request.
     */
    private void handleGetAliases(HttpServletResponse response, PrintWriter out) {
        DatabaseAlias[] aliases = DatabaseAlias.values();
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"aliases\":[");

        for (int i = 0; i < aliases.length; i++) {
            json.append("{");
            json.append("\"alias\":\"").append(aliases[i].getAlias()).append("\",");
            json.append("\"jndiName\":\"").append(aliases[i].getJndiName()).append("\"");
            json.append("}");
            if (i < aliases.length - 1) {
                json.append(",");
            }
        }

        json.append("]}");

        response.setStatus(HttpServletResponse.SC_OK);
        out.write(json.toString());
    }

    /**
     * Handle get transaction status request.
     */
    private void handleGetStatus(HttpServletResponse response, PrintWriter out) {
        boolean hasActive = queryExecutorService.hasActiveTransaction();
        String sessionId = queryExecutorService.getSessionId();

        response.setStatus(HttpServletResponse.SC_OK);
        out.write("{\"success\":true,\"hasActiveTransaction\":" + hasActive +
            ",\"sessionId\":\"" + sessionId + "\"}");
    }

    /**
     * Simple JSON parser for QueryRequest (avoiding external dependencies).
     */
    private QueryRequest parseQueryRequest(String json) {
        try {
            QueryRequest request = new QueryRequest();

            // Extract query
            int queryStart = json.indexOf("\"query\"");
            if (queryStart != -1) {
                int colonIndex = json.indexOf(":", queryStart);
                int quoteStart = json.indexOf("\"", colonIndex);
                int quoteEnd = findClosingQuote(json, quoteStart + 1);
                if (quoteEnd != -1) {
                    String query = json.substring(quoteStart + 1, quoteEnd);
                    request.setQuery(unescapeJson(query));
                }
            }

            // Extract alias
            int aliasStart = json.indexOf("\"alias\"");
            if (aliasStart != -1) {
                int colonIndex = json.indexOf(":", aliasStart);
                int quoteStart = json.indexOf("\"", colonIndex);
                int quoteEnd = findClosingQuote(json, quoteStart + 1);
                if (quoteEnd != -1) {
                    String alias = json.substring(quoteStart + 1, quoteEnd);
                    request.setAlias(unescapeJson(alias));
                }
            }

            return request;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing query request", e);
            return null;
        }
    }

    /**
     * Find closing quote considering escape sequences.
     */
    private int findClosingQuote(String str, int startIndex) {
        for (int i = startIndex; i < str.length(); i++) {
            if (str.charAt(i) == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convert QueryResponse to JSON string.
     */
    private String toJson(QueryResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(response.isSuccess()).append(",");
        json.append("\"affectedRows\":").append(response.getAffectedRows()).append(",");
        json.append("\"message\":\"").append(escapeJson(response.getMessage())).append("\"");

        if (response.getErrorDetails() != null) {
            json.append(",\"errorDetails\":\"").append(escapeJson(response.getErrorDetails())).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Escape special characters for JSON.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Unescape JSON strings.
     */
    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
