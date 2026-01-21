package com.oracle.client.service;

import com.oracle.client.model.User;

import javax.ejb.Local;

/**
 * Service for user authentication.
 */
@Local
public interface AuthenticationService {

    /**
     * Authenticate user with username and password.
     *
     * @param username Username
     * @param password Plain-text password (will be hashed internally)
     * @return User object if authentication successful, null otherwise
     */
    User authenticate(String username, String password);

    /**
     * Get user by username.
     *
     * @param username Username
     * @return User object if found, null otherwise
     */
    User getUserByUsername(String username);

    /**
     * Hash password using SHA-256.
     *
     * @param password Plain-text password
     * @return SHA-256 hash (hex string, 64 characters)
     */
    String hashPassword(String password);
}
