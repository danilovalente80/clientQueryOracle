package com.oracle.client.service;

import com.oracle.client.config.DatabaseAlias;
import com.oracle.client.model.User;

import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless EJB for user authentication using sesamo.client_utenti table.
 * Passwords are hashed using SHA-256.
 */
@Stateless
public class AuthenticationServiceBean implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationServiceBean.class.getName());

    // JNDI for SESAMO database (authentication database)
    private static final String AUTH_DB_JNDI = "jdbc/ds_sesamo";

    @Override
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            LOGGER.warning("Authentication failed: empty username");
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            LOGGER.warning("Authentication failed: empty password");
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Get connection to authentication database (SESAMO)
            conn = getAuthConnection();

            // Query to authenticate user
            String sql =
                "SELECT id_utente, username, nome, cognome, email, " +
                "       data_inizio, data_fine, attivo, password_hash " +
                "FROM sesamo.client_utenti " +
                "WHERE username = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String inputHash = hashPassword(password);

                // Verify password hash
                if (storedHash != null && storedHash.equalsIgnoreCase(inputHash)) {
                    // Password matches - create User object
                    User user = new User();
                    user.setIdUtente(rs.getLong("id_utente"));
                    user.setUsername(rs.getString("username"));
                    user.setNome(rs.getString("nome"));
                    user.setCognome(rs.getString("cognome"));
                    user.setEmail(rs.getString("email"));
                    user.setDataInizio(rs.getDate("data_inizio"));
                    user.setDataFine(rs.getDate("data_fine"));
                    user.setAttivo("S".equals(rs.getString("attivo")));

                    // Check if user is valid (active and within validity dates)
                    if (!user.isValid()) {
                        LOGGER.warning("Authentication failed for user '" + username +
                            "': account not valid (inactive or expired)");
                        return null;
                    }

                    LOGGER.info("User authenticated successfully: " + username);
                    return user;

                } else {
                    LOGGER.warning("Authentication failed for user '" + username + "': invalid password");
                    return null;
                }
            } else {
                LOGGER.warning("Authentication failed: user '" + username + "' not found");
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error during authentication", e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during authentication", e);
            return null;
        } finally {
            closeResources(rs, stmt, conn);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getAuthConnection();

            String sql =
                "SELECT id_utente, username, nome, cognome, email, " +
                "       data_inizio, data_fine, attivo " +
                "FROM sesamo.client_utenti " +
                "WHERE username = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setIdUtente(rs.getLong("id_utente"));
                user.setUsername(rs.getString("username"));
                user.setNome(rs.getString("nome"));
                user.setCognome(rs.getString("cognome"));
                user.setEmail(rs.getString("email"));
                user.setDataInizio(rs.getDate("data_inizio"));
                user.setDataFine(rs.getDate("data_fine"));
                user.setAttivo("S".equals(rs.getString("attivo")));

                return user;
            }

            return null;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user", e);
            return null;
        } finally {
            closeResources(rs, stmt, conn);
        }
    }

    @Override
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Get connection to authentication database (SESAMO).
     */
    private Connection getAuthConnection() throws Exception {
        Context ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(AUTH_DB_JNDI);
        return ds.getConnection();
    }

    /**
     * Close JDBC resources safely.
     */
    private void closeResources(ResultSet rs, PreparedStatement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing ResultSet", e);
            }
        }
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
