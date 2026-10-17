package com.payments.automation.db;

import com.payments.automation.config.ConfigReader;

/**
 * Connection settings read from db.properties using the type prefix.
 * Passwords should come from environment variables: oracle.password=${ORACLE_DB_PASSWORD}
 */
public record DbConfig(String url, String username, String password, String driverClass, int queryTimeoutSeconds) {

    public static DbConfig of(DatabaseType type) {
        return of(type.prefix());
    }

    /** Use a custom prefix to hold several connections of the same type, e.g. "oracle.lm" and "oracle.rtgs". */
    public static DbConfig of(String prefix) {
        return new DbConfig(
                ConfigReader.getRequired(prefix + ".url"),
                ConfigReader.get(prefix + ".username", ""),
                ConfigReader.get(prefix + ".password", ""),
                ConfigReader.get(prefix + ".driver", ""),
                ConfigReader.getInt(prefix + ".query.timeout", 60));
    }

    @Override
    public String toString() {
        return "DbConfig[url=" + url + ", user=" + username + ", password=****]";
    }
}
