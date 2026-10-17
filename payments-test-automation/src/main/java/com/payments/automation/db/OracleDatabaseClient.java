package com.payments.automation.db;

import java.time.LocalDateTime;

/**
 * Oracle Database client (ojdbc17).
 *
 * <pre>
 * oracle.url=jdbc:oracle:thin:@//db-host:1521/SERVICE_NAME
 * oracle.username=LM_APP_RO
 * oracle.password=${ORACLE_DB_PASSWORD}
 * oracle.driver=oracle.jdbc.OracleDriver
 * </pre>
 */
public class OracleDatabaseClient extends DatabaseClient {

    public OracleDatabaseClient(DbConfig config) {
        super(config);
    }

    @Override
    public DatabaseType type() {
        return DatabaseType.ORACLE;
    }

    @Override
    public boolean tableExists(String tableName) {
        return count("USER_TABLES", "TABLE_NAME = ?", tableName.toUpperCase()) > 0;
    }

    /** Checks a table in another schema (needs SELECT on ALL_TABLES). */
    public boolean tableExists(String owner, String tableName) {
        return count("ALL_TABLES", "OWNER = ? AND TABLE_NAME = ?", owner.toUpperCase(), tableName.toUpperCase()) > 0;
    }

    @Override
    public String currentSchema() {
        return queryForString("SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL");
    }

    @Override
    public void setSchema(String schema) {
        executeUpdate("ALTER SESSION SET CURRENT_SCHEMA = " + requireIdentifier(schema));
    }

    @Override
    public long nextSequenceValue(String sequenceName) {
        Object v = querySingleValue("SELECT " + requireIdentifier(sequenceName) + ".NEXTVAL FROM DUAL");
        return ((Number) v).longValue();
    }

    /** Oracle 12c+ row limiting clause. */
    @Override
    public String limit(String sql, int n) {
        return sql + " FETCH FIRST " + n + " ROWS ONLY";
    }

    @Override
    public LocalDateTime databaseTime() {
        Object v = querySingleValue("SELECT SYSTIMESTAMP FROM DUAL");
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return ((java.sql.Timestamp) querySingleValue("SELECT CAST(SYSTIMESTAMP AS TIMESTAMP) FROM DUAL")).toLocalDateTime();
    }

    /** Calls a stored function returning VARCHAR: SELECT PKG.FN(?) FROM DUAL */
    public String callFunction(String functionName, Object... params) {
        String placeholders = String.join(",", java.util.Collections.nCopies(params.length, "?"));
        return queryForString("SELECT " + functionName + "(" + placeholders + ") FROM DUAL", params);
    }

    /** Oracle version banner, useful as a connectivity smoke check. */
    public String version() {
        return queryForString("SELECT BANNER_FULL FROM V$VERSION WHERE ROWNUM = 1");
    }

    static String requireIdentifier(String name) {
        if (!name.matches("[A-Za-z][A-Za-z0-9_$#.]{0,127}")) {
            throw new IllegalArgumentException("Invalid identifier: " + name);
        }
        return name;
    }
}
