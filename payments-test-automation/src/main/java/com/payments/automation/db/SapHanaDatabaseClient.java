package com.payments.automation.db;

import java.time.LocalDateTime;

/**
 * SAP HANA client (ngdbc driver).
 *
 * <pre>
 * hana.url=jdbc:sap://hana-host:30015/?databaseName=HDB&amp;currentschema=PAYMENTS&amp;encrypt=true
 * hana.username=TEST_USER
 * hana.password=${HANA_DB_PASSWORD}
 * hana.driver=com.sap.db.jdbc.Driver
 * </pre>
 * Port convention: 3&lt;instance&gt;15 (single container) or 3&lt;instance&gt;13/41 for tenant DBs.
 */
public class SapHanaDatabaseClient extends DatabaseClient {

    public SapHanaDatabaseClient(DbConfig config) {
        super(config);
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.SAP_HANA;
    }

    @Override
    public boolean tableExists(String tableName) {
        return count("SYS.TABLES", "SCHEMA_NAME = CURRENT_SCHEMA AND TABLE_NAME = ?", tableName.toUpperCase()) > 0;
    }

    public boolean tableExists(String schema, String tableName) {
        return count("SYS.TABLES", "SCHEMA_NAME = ? AND TABLE_NAME = ?", schema.toUpperCase(), tableName.toUpperCase()) > 0;
    }

    /** HANA calculation / SQL views live in SYS.VIEWS, not SYS.TABLES. */
    public boolean viewExists(String schema, String viewName) {
        return count("SYS.VIEWS", "SCHEMA_NAME = ? AND VIEW_NAME = ?", schema, viewName) > 0;
    }

    @Override
    public String currentSchema() {
        return queryForString("SELECT CURRENT_SCHEMA FROM DUMMY");
    }

    @Override
    public void setSchema(String schema) {
        executeUpdate("SET SCHEMA " + OracleDatabaseClient.requireIdentifier(schema));
    }

    @Override
    public long nextSequenceValue(String sequenceName) {
        Object v = querySingleValue("SELECT " + OracleDatabaseClient.requireIdentifier(sequenceName) + ".NEXTVAL FROM DUMMY");
        return ((Number) v).longValue();
    }

    @Override
    public String limit(String sql, int n) {
        return sql + " LIMIT " + n;
    }

    @Override
    public LocalDateTime databaseTime() {
        return ((java.sql.Timestamp) querySingleValue("SELECT CURRENT_TIMESTAMP FROM DUMMY")).toLocalDateTime();
    }

    public String version() {
        return queryForString("SELECT VERSION FROM SYS.M_DATABASE");
    }
}
