package com.oracle.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for splitting multiple SQL queries separated by semicolons.
 */
public class QuerySplitter {

    /**
     * Split SQL text into individual queries separated by semicolons.
     * Handles semicolons inside strings correctly.
     *
     * @param sqlText The SQL text containing one or more queries
     * @return List of individual SQL queries (trimmed, non-empty)
     */
    public static List<String> splitQueries(String sqlText) {
        List<String> queries = new ArrayList<String>();

        if (sqlText == null || sqlText.trim().isEmpty()) {
            return queries;
        }

        StringBuilder currentQuery = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sqlText.length(); i++) {
            char c = sqlText.charAt(i);
            char prev = i > 0 ? sqlText.charAt(i - 1) : '\0';

            // Handle single quotes
            if (c == '\'' && !inDoubleQuote && prev != '\\') {
                inSingleQuote = !inSingleQuote;
                currentQuery.append(c);
            }
            // Handle double quotes
            else if (c == '"' && !inSingleQuote && prev != '\\') {
                inDoubleQuote = !inDoubleQuote;
                currentQuery.append(c);
            }
            // Handle semicolon (query separator)
            else if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                // End of query - add to list if not empty
                String query = currentQuery.toString().trim();
                if (!query.isEmpty()) {
                    queries.add(query);
                }
                currentQuery = new StringBuilder();
            }
            // Regular character
            else {
                currentQuery.append(c);
            }
        }

        // Add last query if present
        String lastQuery = currentQuery.toString().trim();
        if (!lastQuery.isEmpty()) {
            queries.add(lastQuery);
        }

        return queries;
    }

    /**
     * Check if text contains multiple queries.
     *
     * @param sqlText The SQL text to check
     * @return true if contains multiple queries, false otherwise
     */
    public static boolean hasMultipleQueries(String sqlText) {
        return splitQueries(sqlText).size() > 1;
    }
}
