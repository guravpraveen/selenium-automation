package com.payments.automation.stepdefs;

import com.payments.automation.context.TestContext;
import com.payments.automation.db.DatabaseFactory;
import com.payments.automation.utils.FileUtils;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public class DatabaseSteps {

    private final TestContext ctx;

    public DatabaseSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    /** "practice" -> H2, "liquidity" -> Oracle, "cbpr" -> SAP HANA (see app.*.db in config.properties). */
    @Given("I am connected to the {string} application database")
    public void connect(String application) {
        ctx.db = DatabaseFactory.forApplication(application);
        Assertions.assertThat(ctx.db.getConnection()).isNotNull();
    }

    @Given("the practice payments data is loaded")
    public void loadPracticeData() {
        ctx.db.executeScript(FileUtils.readResource("db/h2-schema.sql"));
    }

    @When("I fetch the payment with reference {string}")
    public void fetchPayment(String reference) {
        ctx.dbRows = ctx.db.executeQuery("SELECT * FROM PAYMENTS WHERE TRN_REF = ?", reference);
    }

    @When("the payment {string} status is updated to {string}")
    public void updateStatus(String reference, String status) {
        int rows = ctx.db.executeUpdate("UPDATE PAYMENTS SET STATUS = ? WHERE TRN_REF = ?", status, reference);
        Assertions.assertThat(rows).isEqualTo(1);
    }

    @Then("the payment record should have:")
    public void verifyRecord(DataTable expected) {
        Assertions.assertThat(ctx.dbRows).as("payment rows").hasSize(1);
        Map<String, Object> actual = ctx.dbRows.get(0);
        expected.asMap(String.class, String.class).forEach((col, value) -> {
            Object a = actual.get(String.valueOf(col).toUpperCase());
            if (a instanceof BigDecimal bd) {
                Assertions.assertThat(bd).as(String.valueOf(col)).isEqualByComparingTo(String.valueOf(value));
            } else {
                Assertions.assertThat(String.valueOf(a)).as(String.valueOf(col)).isEqualTo(value);
            }
        });
    }

    @Then("the payment {string} should reach status {string} within {int} seconds")
    public void waitForStatus(String reference, String status, int seconds) {
        boolean reached = ctx.db.waitForValue("SELECT STATUS FROM PAYMENTS WHERE TRN_REF = ?", status,
                Duration.ofSeconds(seconds), Duration.ofMillis(500), reference);
        Assertions.assertThat(reached).as("status " + status).isTrue();
    }

    @Then("there should be {int} payments with scheme {string}")
    public void countByScheme(int expected, String scheme) {
        Assertions.assertThat(ctx.db.count("PAYMENTS", "SCHEME = ?", scheme)).isEqualTo(expected);
    }

    @Then("the nostro account {string} balance should stay above threshold after debit of {bigdecimal}")
    public void liquidity(String account, BigDecimal debit) {
        Map<String, Object> pos = ctx.db.queryForRow(
                "SELECT CURRENT_BAL, THRESHOLD FROM LIQUIDITY_POSITIONS WHERE ACCOUNT_NO = ?", account);
        BigDecimal after = ((BigDecimal) pos.get("CURRENT_BAL")).subtract(debit);
        Assertions.assertThat(after).isGreaterThan((BigDecimal) pos.get("THRESHOLD"));
    }
}
