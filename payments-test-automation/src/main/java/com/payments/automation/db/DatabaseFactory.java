package com.payments.automation.db;

import com.payments.automation.config.ConfigReader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks the right database per application under test.
 *
 * <pre>
 * # config.properties / db.properties
 * db.type=oracle                 # default when no application is given
 * app.liquidity.db=oracle        # Liquidity Management -> Oracle
 * app.cbpr.db=hana               # CBPR+ payments hub     -> SAP HANA
 * app.rtgs.db=oracle.rtgs        # a second Oracle connection using the "oracle.rtgs.*" keys
 * </pre>
 *
 * Usage: {@code DatabaseClient db = DatabaseFactory.forApplication("cbpr");}
 */
public final class DatabaseFactory {

    private static final Map<String, DatabaseClient> CLIENTS = new ConcurrentHashMap<>();

    private DatabaseFactory() {
    }

    /** Database configured by {@code db.type} (-Ddb.type=hana overrides). */
    public static DatabaseClient getDefault() {
        return forPrefix(ConfigReader.get("db.type", "h2"));
    }

    /** Database mapped to an application by {@code app.<name>.db}. */
    public static DatabaseClient forApplication(String application) {
        String prefix = ConfigReader.get("app." + application.toLowerCase() + ".db");
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("No database mapped for application '" + application
                    + "'. Add app." + application.toLowerCase() + ".db=oracle|hana|h2 to config.");
        }
        return forPrefix(prefix);
    }

    public static DatabaseClient forType(DatabaseType type) {
        return forPrefix(type.prefix());
    }

    /**
     * Prefix = db type, optionally followed by a connection name: "oracle", "hana", "oracle.rtgs".
     * Clients are cached and reused so tests do not open a new connection each time.
     */
    public static DatabaseClient forPrefix(String prefix) {
        return CLIENTS.computeIfAbsent(prefix, p -> {
            DatabaseType type = DatabaseType.from(p.contains(".") ? p.substring(0, p.indexOf('.')) : p);
            DbConfig config = DbConfig.of(p);
            return switch (type) {
                case ORACLE -> new OracleDatabaseClient(config);
                case SAP_HANA -> new SapHanaDatabaseClient(config);
                case H2 -> new H2DatabaseClient(config);
            };
        });
    }

    public static void closeAll() {
        CLIENTS.values().forEach(DatabaseClient::close);
        CLIENTS.clear();
    }
}
