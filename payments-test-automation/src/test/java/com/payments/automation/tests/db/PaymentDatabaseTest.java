package com.payments.automation.tests.db;

import com.payments.automation.db.DatabaseClient;
import com.payments.automation.db.DatabaseFactory;
import com.payments.automation.db.DatabaseType;
import com.payments.automation.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Database tests against the in-memory H2 "practice" application (app.practice.db=h2).
 * Point the same tests at Oracle or HANA by changing the application name or -Ddb.type.
 */
public class PaymentDatabaseTest {

    private DatabaseClient db;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        db = DatabaseFactory.forApplication("practice");
        db.executeScript(FileUtils.readResource("db/h2-schema.sql"));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DatabaseFactory.closeAll();
    }

    @Test(groups = {"db", "smoke"})
    public void connectionAndMetadata() {
        Assertions.assertThat(db.type()).isEqualTo(DatabaseType.H2);
        Assertions.assertThat(db.isConnected()).isTrue();
        Assertions.assertThat(db.tableExists("PAYMENTS")).isTrue();
        Assertions.assertThat(db.tableExists("NOT_A_TABLE")).isFalse();
        Assertions.assertThat(db.currentSchema()).isNotBlank();
        Assertions.assertThat(db.databaseTime()).isNotNull();
    }

    @Test(groups = "db")
    public void queryPaymentByUetr() {
        Map<String, Object> row = db.queryForRow(
                "SELECT TRN_REF, CCY, AMOUNT, STATUS FROM PAYMENTS WHERE UETR = ?",
                "e6f1a2b3-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
        Assertions.assertThat(row).containsEntry("TRN_REF", "TRN0001").containsEntry("STATUS", "SETTLED");
        Assertions.assertThat((BigDecimal) row.get("AMOUNT")).isEqualByComparingTo("25000.00");
    }

    @Test(groups = "db")
    public void singleValueColumnAndCount() {
        Assertions.assertThat(db.queryForString("SELECT STATUS FROM PAYMENTS WHERE TRN_REF = ?", "TRN0002"))
                .isEqualTo("PENDING");
        List<Object> currencies = db.queryColumn("SELECT DISTINCT CCY FROM PAYMENTS ORDER BY CCY");
        Assertions.assertThat(currencies).containsExactly("HKD", "INR", "USD");
        Assertions.assertThat(db.count("PAYMENTS", "SCHEME = ?", "CBPR")).isEqualTo(2);
        Assertions.assertThat(db.getColumnNames("SELECT * FROM PAYMENTS")).contains("UETR", "STATUS", "VALUE_DATE");
    }

    @Test(groups = "db")
    public void insertUpdateDeleteWithSequence() {
        long id = db.nextSequenceValue("PAYMENT_SEQ");
        String uetr = UUID.randomUUID().toString();
        int inserted = db.executeUpdate(
                "INSERT INTO PAYMENTS (PAYMENT_ID, TRN_REF, UETR, MSG_TYPE, SCHEME, SENDER_BIC, RECEIVER_BIC, CCY, AMOUNT, VALUE_DATE, STATUS) "
                        + "VALUES (?, ?, ?, 'pacs.008', 'CBPR', 'PAYBGB2LXXX', 'PAYBSGSGXXX', 'USD', ?, ?, 'RECEIVED')",
                id, "TRN" + id, uetr, new BigDecimal("999.99"), LocalDate.now());
        Assertions.assertThat(inserted).isEqualTo(1);

        Assertions.assertThat(db.executeUpdate("UPDATE PAYMENTS SET STATUS = ? WHERE UETR = ?", "VALIDATED", uetr)).isEqualTo(1);
        Assertions.assertThat(db.queryForString("SELECT STATUS FROM PAYMENTS WHERE UETR = ?", uetr)).isEqualTo("VALIDATED");

        Assertions.assertThat(db.executeUpdate("DELETE FROM PAYMENTS WHERE UETR = ?", uetr)).isEqualTo(1);
        Assertions.assertThat(db.exists("SELECT 1 FROM PAYMENTS WHERE UETR = ?", uetr)).isFalse();
    }

    @Test(groups = "db")
    public void batchInsertAndTransactionRollback() {
        int before = db.count("PAYMENT_AUDIT", null);
        int[] result = db.executeBatch("INSERT INTO PAYMENT_AUDIT (PAYMENT_ID, OLD_STATUS, NEW_STATUS) VALUES (?, ?, ?)",
                List.of(new Object[]{1, "RECEIVED", "VALIDATED"}, new Object[]{1, "VALIDATED", "SETTLED"}));
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(db.count("PAYMENT_AUDIT", null)).isEqualTo(before + 2);

        Assertions.assertThatThrownBy(() -> db.inTransaction(tx -> {
            tx.executeUpdate("INSERT INTO PAYMENT_AUDIT (PAYMENT_ID, OLD_STATUS, NEW_STATUS) VALUES (2, 'A', 'B')");
            tx.executeUpdate("INSERT INTO NO_SUCH_TABLE VALUES (1)"); // fails -> whole transaction rolled back
        })).isInstanceOf(RuntimeException.class);
        Assertions.assertThat(db.count("PAYMENT_AUDIT", null)).isEqualTo(before + 2);
    }

    @Test(groups = "db", description = "Liquidity: current balance stays above threshold after debit")
    public void liquidityPositionAboveThreshold() {
        Map<String, Object> pos = db.queryForRow(
                "SELECT CURRENT_BAL, THRESHOLD FROM LIQUIDITY_POSITIONS WHERE ACCOUNT_NO = ?", "NOSTRO-USD-001");
        BigDecimal current = (BigDecimal) pos.get("CURRENT_BAL");
        BigDecimal threshold = (BigDecimal) pos.get("THRESHOLD");
        Assertions.assertThat(current.subtract(new BigDecimal("25000.00"))).isGreaterThan(threshold);
    }

    @Test(groups = "db", description = "Polling for an asynchronous status change")
    public void waitForStatus() {
        db.executeUpdate("INSERT INTO PAYMENTS (PAYMENT_ID, TRN_REF, UETR, MSG_TYPE, SCHEME, SENDER_BIC, RECEIVER_BIC, CCY, AMOUNT, VALUE_DATE, STATUS) "
                + "VALUES (?, 'TRNWAIT', ?, 'pacs.009', 'RTGS', 'PAYBINBBXXX', 'CNTRINBBXXX', 'INR', 100, ?, 'PENDING')",
                db.nextSequenceValue("PAYMENT_SEQ"), UUID.randomUUID().toString(), LocalDate.now());
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            // separate client = separate connection, like the payment engine updating the row
            try (DatabaseClient engine = new com.payments.automation.db.H2DatabaseClient(
                    com.payments.automation.db.DbConfig.of("h2"))) {
                engine.executeUpdate("UPDATE PAYMENTS SET STATUS = 'SETTLED' WHERE TRN_REF = 'TRNWAIT'");
            }
        }).start();
        boolean settled = db.waitForValue("SELECT STATUS FROM PAYMENTS WHERE TRN_REF = ?", "SETTLED",
                Duration.ofSeconds(10), Duration.ofMillis(300), "TRNWAIT");
        Assertions.assertThat(settled).isTrue();
        db.executeUpdate("DELETE FROM PAYMENTS WHERE TRN_REF = 'TRNWAIT'");
    }

    @Test(groups = "db")
    public void dbSpecificLimitSyntax() {
        List<Map<String, Object>> rows = db.executeQuery(db.limit("SELECT * FROM PAYMENTS ORDER BY PAYMENT_ID", 2));
        Assertions.assertThat(rows).hasSize(2);
    }
}
