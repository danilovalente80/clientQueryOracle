package com.oracle.client.config;

/**
 * Enum for mapping database aliases to JNDI datasource names.
 * Each alias corresponds to a specific Oracle database connection configured in WebSphere.
 */
public enum DatabaseAlias {

    SESAMO("SESAMO", "jdbc/ds_sesamo"),
    SIGN_DCM("SIGN_DCM", "jdbc/ws_signapp"),
    EJBSCHED_ENTR("EJBSCHED_ENTR", "jdbc/DBTASK"),
    MON_ASP("MON_ASP", "jdbc/ds_MONASP"),
    
    
    ENTR_ASP("ENTR_ASP", "jdbc/nsd_entr"),
    
    FASC_EQUI("FASC_EQUI", "jdbc/gaiweb_equi"),  
    FASC_AAMS("FASC_AAMS", "jdbc/gaiweb_aams"),  
    FASC_ACN("FASC_ACN", "jdbc/gaiweb_acn"),  
    FASC_ADER("FASC_ADER", "jdbc/gaiweb_ader"),  
    FASC_CONSIP("xxx", "jdbc/gaiweb_consip"),  
    /*
    xxx("xxx", "jdbc/gaiweb_demanio"),  
    xxx("xxx", "jdbc/gaiweb_entr"),  
    xxx("xxx", "jdbc/gaiweb_sogei"),  
    xxx("xxx", "jdbc/gta_equi"),  
    xxx("xxx", "jdbc/gta_aams"),  
    xxx("xxx", "jdbc/gta_ader"),  
    xxx("xxx", "jdbc/gta_consip"),  
    xxx("xxx", "jdbc/gta_demanio"),  
    xxx("xxx", "jdbc/gta_entr"),  
    xxx("xxx", "jdbc/gta_sogei"),  
    xxx("xxx", "jdbc/nsd_equi"),  
    xxx("xxx", "jdbc/nsd_aams"),  
    xxx("xxx", "jdbc/nsd_ader"),  
    xxx("xxx", "jdbc/nsd_consip"),  
    xxx("xxx", "jdbc/nsd_demanio"), 
    xxx("xxx", "jdbc/nsd_entr"),  
    xxx("xxx", "jdbc/nsd_sogei"),  
     
    */

    xxx("xxx", "jdbc/xxx");
    
	

    private final String alias;
    private final String jndiName;

    /**
     * Constructor for DatabaseAlias enum.
     *
     * @param alias The alias used in queries (e.g., "entr_asp")
     * @param jndiName The JNDI name configured in WebSphere (e.g., "jdbc/EntrAspDS")
     */
    DatabaseAlias(String alias, String jndiName) {
        this.alias = alias;
        this.jndiName = jndiName;
    }

    /**
     * Get the alias name.
     *
     * @return The alias string
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get the JNDI name for this alias.
     *
     * @return The JNDI datasource name
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Find DatabaseAlias by alias string (case-insensitive).
     *
     * @param alias The alias to search for
     * @return The corresponding DatabaseAlias
     * @throws IllegalArgumentException if alias is not found
     */
    public static DatabaseAlias fromAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Alias cannot be null or empty");
        }

        String normalizedAlias = alias.trim().toLowerCase();

        for (DatabaseAlias dbAlias : DatabaseAlias.values()) {
            if (dbAlias.getAlias().equalsIgnoreCase(normalizedAlias)) {
                return dbAlias;
            }
        }

        throw new IllegalArgumentException("Unknown database alias: " + alias +
            ". Available aliases: " + getAvailableAliases());
    }

    /**
     * Check if an alias exists.
     *
     * @param alias The alias to check
     * @return true if the alias exists, false otherwise
     */
    public static boolean isValidAlias(String alias) {
        try {
            fromAlias(alias);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get a comma-separated list of all available aliases.
     *
     * @return String containing all available aliases
     */
    public static String getAvailableAliases() {
        StringBuilder sb = new StringBuilder();
        DatabaseAlias[] values = DatabaseAlias.values();
        for (int i = 0; i < values.length; i++) {
            sb.append(values[i].getAlias());
            if (i < values.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "DatabaseAlias{" +
                "alias='" + alias + '\'' +
                ", jndiName='" + jndiName + '\'' +
                '}';
    }
}
