package com.payments.automation.db;

import java.time.LocalDateTime;

/**
 * In-memory H2 database (Oracle compatibility mode) so the DB layer can be
 * practised without access to a real Oracle / HANA instance.
 */
public class H2DatabaseClient extends DatabaseClient {

    public H2DatabaseClient(DbConfig config) {
        super(config);
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.H2;
    }

    @Override
    public boolean tableExists(String tableName) {
        return count("INFORMATION_SCHEMA.TABLES", "TABLE_NAME = ?", tableName.toUpperCase()) > 0;
    }

    @Override
    public String currentSchema() {
        return queryForString("SELECT CURRENT_SCHEMA");
    }

    @Override
    public void setSchema(String schema) {
        executeUpdate("SET SCHEMA " + OracleDatabaseClient.requireIdentifier(schema));
    }

    @Override
    public long nextSequenceValue(String sequenceName) {
        return ((Number) querySingleValue("SELECT NEXT VALUE FOR " + OracleDatabaseClient.requireIdentifier(sequenceName)))
                .longValue();
    }

    @Override
    public String limit(String sql, int n) {
        return sql + " FETCH FIRST " + n + " ROWS ONLY";
    }

    @Override
    public LocalDateTime databaseTime() {
        return ((java.sql.Timestamp) querySingleValue("SELECT CAST(CURRENT_TIMESTAMP AS TIMESTAMP)")).toLocalDateTime();
    }
}
