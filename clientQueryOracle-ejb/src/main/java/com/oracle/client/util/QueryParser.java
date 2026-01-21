package com.oracle.client.util;

import com.oracle.client.config.DatabaseAlias;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing SQL queries and extracting database aliases.
 *
 * This parser extracts schema names from queries like:
 * - UPDATE sesamo.uffici SET ...
 * - INSERT INTO entr_asp.tabella ...
 * - DELETE FROM sesamo.records WHERE ...
 */
public class QueryParser {

    // Pattern to match schema.table references in SQL queries
    // Matches: UPDATE/INSERT INTO/DELETE FROM schema.table
    private static final Pattern SCHEMA_PATTERN = Pattern.compile(
        "(?:UPDATE|INSERT\\s+INTO|DELETE\\s+FROM|FROM|JOIN)\\s+" +
        "([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Extract database alias from SQL query.
     *
     * Looks for schema.table patterns in the query and tries to match
     * the schema name with configured database aliases.
     *
     * Examples:
     * - "UPDATE sesamo.uffici SET ..." → returns "sesamo"
     * - "INSERT INTO entr_asp.table1 VALUES ..." → returns "entr_asp"
     * - "DELETE FROM sesamo.records WHERE ..." → returns "sesamo"
     *
     * @param query The SQL query to parse
     * @return The extracted alias, or null if no valid alias found
     */
    public static String extractAlias(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        // Find all schema.table patterns
        Matcher matcher = SCHEMA_PATTERN.matcher(query);

        while (matcher.find()) {
            String schemaName = matcher.group(1);

            // Check if this schema name is a valid alias
            if (DatabaseAlias.isValidAlias(schemaName)) {
                return schemaName;
            }
        }

        return null;
    }

    /**
     * Check if a query contains a schema qualifier.
     *
     * @param query The SQL query to check
     * @return true if query contains schema.table pattern, false otherwise
     */
    public static boolean containsSchemaQualifier(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        Matcher matcher = SCHEMA_PATTERN.matcher(query);
        return matcher.find();
    }

    /**
     * Get a description of which alias was detected in the query.
     * Useful for logging and error messages.
     *
     * @param query The SQL query
     * @return Description string, or null if no alias detected
     */
    public static String getAliasDetectionInfo(String query) {
        String alias = extractAlias(query);
        if (alias != null) {
            try {
                DatabaseAlias dbAlias = DatabaseAlias.fromAlias(alias);
                return "Detected alias '" + alias + "' → JNDI: " + dbAlias.getJndiName();
            } catch (IllegalArgumentException e) {
                return "Found schema '" + alias + "' but no matching alias configured";
            }
        }

        if (containsSchemaQualifier(query)) {
            return "Query contains schema qualifiers but no matching alias found";
        }

        return "No schema qualifier detected in query";
    }

    /**
     * Extract all schema names from a query (may include non-alias schemas).
     *
     * @param query The SQL query
     * @return Array of schema names found, empty array if none
     */
    public static String[] extractAllSchemas(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new String[0];
        }

        java.util.Set<String> schemas = new java.util.LinkedHashSet<String>();
        Matcher matcher = SCHEMA_PATTERN.matcher(query);

        while (matcher.find()) {
            schemas.add(matcher.group(1));
        }

        return schemas.toArray(new String[schemas.size()]);
    }
}
