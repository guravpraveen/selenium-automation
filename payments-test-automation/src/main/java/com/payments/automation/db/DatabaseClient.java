package com.payments.automation.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Common JDBC operations shared by every database. Always use the {@code ?}
 * parameter placeholders - never concatenate test data into SQL.
 *
 * <p>Implementations: {@link OracleDatabaseClient}, {@link SapHanaDatabaseClient}, {@link H2DatabaseClient}.
 * Obtain one through {@link DatabaseFactory}.</p>
 */
public abstract class DatabaseClient implements AutoCloseable {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final DbConfig config;
    private Connection connection;

    protected DatabaseClient(DbConfig config) {
        this.config = config;
    }

    public abstract DatabaseType type();

    /** True if the table exists in the current schema - query differs per database. */
    public abstract boolean tableExists(String tableName);

    public abstract String currentSchema();

    public abstract void setSchema(String schema);

    /** Next value from a sequence - syntax differs per database. */
    public abstract long nextSequenceValue(String sequenceName);

    /** Wraps a query so that only the first {@code n} rows are returned. */
    public abstract String limit(String sql, int n);

    public abstract java.time.LocalDateTime databaseTime();

    // ------------------------------------------------------------------ connection
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (!config.driverClass().isEmpty()) {
                    Class.forName(config.driverClass());
                }
                log.info("Connecting to {} -> {}", type(), config.url());
                connection = DriverManager.getConnection(config.url(), config.username(), config.password());
            }
            return connection;
        } catch (SQLException | ClassNotFoundException e) {
            throw new DatabaseException("Unable to connect to " + type() + " using " + config, e);
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
                log.info("Closed {} connection", type());
            } catch (SQLException e) {
                log.warn("Error closing connection: {}", e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // ------------------------------------------------------------------ queries
    /** SELECT returning every row as column -> value (column names upper-cased). */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        log.debug("SQL: {} | params: {}", sql, params);
        try (PreparedStatement ps = prepare(sql, params); ResultSet rs = ps.executeQuery()) {
            return toList(rs);
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + sql, e);
        }
    }

    /** First row or an empty map. */
    public Map<String, Object> queryForRow(String sql, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    /** First column of first row, or null - e.g. SELECT STATUS FROM PAYMENTS WHERE UETR = ? */
    public Object querySingleValue(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, params); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getObject(1) : null;
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + sql, e);
        }
    }

    public String queryForString(String sql, Object... params) {
        Object v = querySingleValue(sql, params);
        return v == null ? null : v.toString();
    }

    /** All values of the first column. */
    public List<Object> queryColumn(String sql, Object... params) {
        List<Object> values = new ArrayList<>();
        try (PreparedStatement ps = prepare(sql, params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                values.add(rs.getObject(1));
            }
            return values;
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + sql, e);
        }
    }

    public int count(String table, String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) FROM " + table + (whereClause == null || whereClause.isBlank() ? "" : " WHERE " + whereClause);
        Object v = querySingleValue(sql, params);
        return v == null ? 0 : ((Number) v).intValue();
    }

    public boolean exists(String sql, Object... params) {
        return !executeQuery(sql, params).isEmpty();
    }

    public List<String> getColumnNames(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, params); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                names.add(md.getColumnLabel(i).toUpperCase());
            }
            return names;
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + sql, e);
        }
    }

    // ------------------------------------------------------------------ DML / DDL
    /** INSERT / UPDATE / DELETE / DDL - returns affected rows. */
    public int executeUpdate(String sql, Object... params) {
        log.debug("SQL: {} | params: {}", sql, params);
        try (PreparedStatement ps = prepare(sql, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Update failed: " + sql, e);
        }
    }

    /** Runs the same statement for many parameter sets - fast test-data loading. */
    public int[] executeBatch(String sql, List<Object[]> paramSets) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (Object[] params : paramSets) {
                bind(ps, params);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Batch failed: " + sql, e);
        }
    }

    /** Runs a ';' separated script (simple statements only, no PL/SQL blocks). */
    public void executeScript(String script) {
        for (String stmt : script.split(";")) {
            if (!stmt.isBlank()) {
                executeUpdate(stmt.trim());
            }
        }
    }

    /** Runs several statements as one transaction; rolls back if any fails. */
    public void inTransaction(java.util.function.Consumer<DatabaseClient> work) {
        Connection con = getConnection();
        try {
            boolean auto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                work.accept(this);
                con.commit();
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Transaction failed", e);
        }
    }

    // ------------------------------------------------------------------ stored procedures
    /** Calls a procedure with IN parameters only: callProcedure("PKG_PAYMENTS.RELEASE", "TRN123"). */
    public void callProcedure(String procedureName, Object... inParams) {
        String placeholders = String.join(",", java.util.Collections.nCopies(inParams.length, "?"));
        try (CallableStatement cs = getConnection().prepareCall("{call " + procedureName + "(" + placeholders + ")}")) {
            bind(cs, inParams);
            cs.execute();
        } catch (SQLException e) {
            throw new DatabaseException("Procedure failed: " + procedureName, e);
        }
    }

    /** Calls a procedure whose last parameter is a VARCHAR OUT parameter and returns it. */
    public String callProcedureWithOut(String procedureName, Object... inParams) {
        String placeholders = String.join(",", java.util.Collections.nCopies(inParams.length + 1, "?"));
        try (CallableStatement cs = getConnection().prepareCall("{call " + procedureName + "(" + placeholders + ")}")) {
            bind(cs, inParams);
            cs.registerOutParameter(inParams.length + 1, Types.VARCHAR);
            cs.execute();
            return cs.getString(inParams.length + 1);
        } catch (SQLException e) {
            throw new DatabaseException("Procedure failed: " + procedureName, e);
        }
    }

    // ------------------------------------------------------------------ polling
    /**
     * Polls until a query returns the expected value - payment engines update status
     * asynchronously (e.g. RECEIVED -> VALIDATED -> SETTLED).
     */
    public boolean waitForValue(String sql, Object expected, Duration timeout, Duration interval, Object... params) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        Object actual = null;
        while (System.currentTimeMillis() < end) {
            actual = querySingleValue(sql, params);
            if (Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
                return true;
            }
            try {
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("Expected '{}' but last value was '{}' after {}", expected, actual, timeout);
        return false;
    }

    // ------------------------------------------------------------------ helpers
    protected PreparedStatement prepare(String sql, Object... params) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setQueryTimeout(config.queryTimeoutSeconds());
        bind(ps, params);
        return ps;
    }

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof java.time.LocalDate d) {
                ps.setDate(i + 1, java.sql.Date.valueOf(d));
            } else if (p instanceof java.time.LocalDateTime dt) {
                ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(dt));
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    protected static List<Map<String, Object>> toList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i).toUpperCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
