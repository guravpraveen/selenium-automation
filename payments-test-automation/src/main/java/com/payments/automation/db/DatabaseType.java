package com.payments.automation.db;

public enum DatabaseType {
    ORACLE("oracle"),
    SAP_HANA("hana"),
    H2("h2");

    /** Prefix used in db.properties, e.g. oracle.url / hana.url / h2.url. */
    private final String prefix;

    DatabaseType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public static DatabaseType from(String value) {
        String v = value.trim().toLowerCase();
        for (DatabaseType t : values()) {
            if (t.prefix.equals(v) || t.name().equalsIgnoreCase(v)) {
                return t;
            }
        }
        if (v.equals("saphana") || v.equals("sap-hana")) {
            return SAP_HANA;
        }
        throw new IllegalArgumentException("Unknown database type: " + value + " (use oracle | hana | h2)");
    }
}
