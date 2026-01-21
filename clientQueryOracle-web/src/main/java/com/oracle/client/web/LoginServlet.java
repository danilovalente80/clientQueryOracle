package com.oracle.client.web;

import com.oracle.client.model.User;
import com.oracle.client.service.AuthenticationService;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for handling user authentication (login/logout).
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/api/auth/*"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final long serialVersionUID = 1L;

    public static final String SESSION_USER_KEY = "authenticated_user";

    @EJB
    private AuthenticationService authenticationService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/login")) {
                handleLogin(request, response, out);
            } else if (pathInfo.equals("/logout")) {
                handleLogout(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"message\":\"Endpoint not found\"}");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing authentication request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Internal server error\"}");
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
            if (pathInfo != null && pathInfo.equals("/check")) {
                handleCheckSession(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"message\":\"Endpoint not found\"}");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GET request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Internal server error\"}");
        }
    }

    /**
     * Handle login request.
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws IOException {

        // Parse JSON request manually
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            sb.append(line);
        }

        String requestBody = sb.toString();
        LOGGER.info("Login request received");

        // Extract username and password from JSON
        String username = extractJsonValue(requestBody, "username");
        String password = extractJsonValue(requestBody, "password");

        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Username is required\"}");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Password is required\"}");
            return;
        }

        // Authenticate user
        User user = authenticationService.authenticate(username, password);

        if (user != null && user.isValid()) {
            // Authentication successful - create session
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER_KEY, user);
            session.setMaxInactiveInterval(1800); // 30 minutes

            LOGGER.info("User logged in successfully: " + username);

            response.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"success\":true," +
                "\"message\":\"Login successful\"," +
                "\"username\":\"" + escapeJson(user.getUsername()) + "\"," +
                "\"displayName\":\"" + escapeJson(user.getNomeCompleto()) + "\"}");

        } else {
            // Authentication failed
            LOGGER.warning("Login failed for user: " + username);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"success\":false," +
                "\"message\":\"Invalid username or password, or account expired/inactive\"}");
        }
    }

    /**
     * Handle logout request.
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            User user = (User) session.getAttribute(SESSION_USER_KEY);
            if (user != null) {
                LOGGER.info("User logged out: " + user.getUsername());
            }
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        out.write("{\"success\":true,\"message\":\"Logout successful\"}");
    }

    /**
     * Handle session check request.
     */
    private void handleCheckSession(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            User user = (User) session.getAttribute(SESSION_USER_KEY);

            if (user != null && user.isValid()) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true," +
                    "\"authenticated\":true," +
                    "\"username\":\"" + escapeJson(user.getUsername()) + "\"," +
                    "\"displayName\":\"" + escapeJson(user.getNomeCompleto()) + "\"}");
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
        out.write("{\"success\":true,\"authenticated\":false}");
    }

    /**
     * Extract value from simple JSON string.
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int quoteStart = json.indexOf("\"", colonIndex);
            if (quoteStart == -1) {
                return null;
            }

            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            if (quoteEnd == -1) {
                return null;
            }

            return json.substring(quoteStart + 1, quoteEnd);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting JSON value for key: " + key, e);
            return null;
        }
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
     * Get authenticated user from session.
     * Utility method for other servlets.
     */
    public static User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute(SESSION_USER_KEY);
        }
        return null;
    }
}
