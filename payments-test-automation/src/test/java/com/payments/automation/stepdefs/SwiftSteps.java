package com.payments.automation.stepdefs;

import com.payments.automation.context.TestContext;
import com.payments.automation.reports.ExtentTestManager;
import com.payments.automation.swift.MtBuilder;
import com.payments.automation.swift.MtMxConverter;
import com.payments.automation.swift.MtParser;
import com.payments.automation.swift.MtValidator;
import com.payments.automation.swift.MxBuilder;
import com.payments.automation.swift.MxMessage;
import com.payments.automation.swift.MxValidator;
import com.payments.automation.swift.PaymentInstruction;
import com.payments.automation.utils.FileUtils;
import com.payments.automation.utils.TestDataGenerator;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public class SwiftSteps {

    private final TestContext ctx;

    public SwiftSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------ Given
    @Given("the MT message file {string}")
    public void mtFile(String resource) {
        ctx.rawMessage = FileUtils.readResource(resource);
        ctx.mtMessage = MtParser.parse(ctx.rawMessage);
        ExtentTestManager.code("MT input", ctx.rawMessage);
    }

    @Given("the MX message file {string}")
    public void mxFile(String resource) {
        ctx.rawMessage = FileUtils.readResource(resource);
        ctx.mxMessage = MxMessage.parse(ctx.rawMessage);
    }

    @Given("a {word} payment with details:")
    public void payment(String scheme, DataTable table) {
        Map<String, String> d = table.asMap(String.class, String.class);
        ctx.payment = new PaymentInstruction()
                .scheme(PaymentInstruction.Scheme.valueOf(scheme.toUpperCase().replace("+", "_PLUS")))
                .reference(d.get("reference"))
                .relatedReference(d.get("relatedReference"))
                .uetr(d.getOrDefault("uetr", TestDataGenerator.uetr()))
                .senderBic(d.get("senderBic"))
                .receiverBic(d.get("receiverBic"))
                .currency(d.get("currency"))
                .amount(d.get("amount"))
                .valueDate(d.containsKey("valueDate") ? LocalDate.parse(d.get("valueDate")) : LocalDate.now())
                .debtorName(d.get("debtorName"))
                .debtorAccount(d.get("debtorAccount"))
                .creditorName(d.get("creditorName"))
                .creditorAccount(d.get("creditorAccount"))
                .remittanceInfo(d.get("remittance"))
                .clearingSystem(d.get("clearingSystem"))
                .charges(PaymentInstruction.Charges.valueOf(d.getOrDefault("charges", "SHA")));
        if (d.containsKey("debtorAddress")) {
            ctx.payment.debtorAddress(Arrays.asList(d.get("debtorAddress").split("\\|")));
        }
        if (d.containsKey("creditorAddress")) {
            ctx.payment.creditorAddress(Arrays.asList(d.get("creditorAddress").split("\\|")));
        }
    }

    // ------------------------------------------------------------------ When
    @When("I generate the MT{int} message")
    public void generateMt(int type) {
        ctx.mtMessage = switch (type) {
            case 103 -> MtBuilder.mt103(ctx.payment);
            case 202 -> MtBuilder.mt202(ctx.payment);
            default -> throw new IllegalArgumentException("MT" + type + " not supported");
        };
        ctx.rawMessage = ctx.mtMessage.toFin();
        ExtentTestManager.code("Generated MT" + type, ctx.rawMessage);
    }

    @When("I generate the ISO 20022 {word} message")
    public void generateMx(String msg) {
        ctx.rawMessage = switch (msg) {
            case "pacs.008" -> MxBuilder.pacs008(ctx.payment);
            case "pacs.009" -> MxBuilder.pacs009(ctx.payment);
            default -> throw new IllegalArgumentException(msg + " not supported");
        };
        ctx.mxMessage = MxMessage.parse(ctx.rawMessage);
        ExtentTestManager.xml("Generated " + msg, ctx.rawMessage);
    }

    @When("I validate the MT message")
    public void validateMt() {
        ctx.validationResult = MtValidator.validate(MtParser.parse(ctx.mtMessage.toFin()));
        ExtentTestManager.info("MT validation: " + ctx.validationResult);
    }

    @When("I validate the MX message against {word} rules")
    public void validateMx(String scheme) {
        PaymentInstruction.Scheme s = PaymentInstruction.Scheme.valueOf(scheme.toUpperCase().replace("+", "_PLUS"));
        ctx.validationResult = MxValidator.validate(ctx.mxMessage, s);
        ExtentTestManager.info("MX validation: " + ctx.validationResult);
    }

    @When("I translate the MT message to MX")
    public void translateMtToMx() {
        ctx.rawMessage = switch (ctx.mtMessage.getMessageType()) {
            case "103" -> MtMxConverter.mt103ToPacs008(ctx.mtMessage.toFin());
            case "202" -> MtMxConverter.mt202ToPacs009(ctx.mtMessage.toFin());
            default -> throw new IllegalArgumentException("No MX mapping for MT" + ctx.mtMessage.getMessageType());
        };
        ctx.mxMessage = MxMessage.parse(ctx.rawMessage);
        ExtentTestManager.xml("Translated MX", ctx.rawMessage);
    }

    @When("I translate the MX message to MT")
    public void translateMxToMt() {
        ctx.mtMessage = switch (ctx.mxMessage.getMessageType()) {
            case "pacs.008" -> MtMxConverter.pacs008ToMt103(ctx.rawMessage);
            case "pacs.009" -> MtMxConverter.pacs009ToMt202(ctx.rawMessage);
            default -> throw new IllegalArgumentException("No MT mapping for " + ctx.mxMessage.getMessageType());
        };
        ExtentTestManager.code("Translated MT", ctx.mtMessage.toFin());
    }

    @When("I change MX element {string} to {string}")
    public void mutateMx(String path, String value) {
        ctx.mxMessage = MxMessage.parse(ctx.mxMessage.set(path, value).toXml());
    }

    // ------------------------------------------------------------------ Then
    @Then("the message should be valid")
    public void valid() {
        Assertions.assertThat(ctx.validationResult.getErrors()).as("validation errors").isEmpty();
    }

    @Then("the message should be rejected with error {string}")
    public void rejected(String code) {
        Assertions.assertThat(ctx.validationResult.hasError(code)).as(ctx.validationResult.toString()).isTrue();
    }

    @Then("the MT field {string} should be {string}")
    public void mtField(String tag, String expected) {
        Assertions.assertThat(ctx.mtMessage.getFieldOrEmpty(tag)).isEqualTo(expected);
    }

    @Then("the MT message type should be {string}")
    public void mtType(String type) {
        Assertions.assertThat(ctx.mtMessage.getMessageType()).isEqualTo(type);
    }

    @Then("the MX element {string} should be {string}")
    public void mxElement(String path, String expected) {
        Assertions.assertThat(ctx.mxMessage.get(path)).as(path).isEqualTo(expected);
    }

    @Then("the MX elements should be:")
    public void mxElements(DataTable table) {
        org.assertj.core.api.SoftAssertions soft = new org.assertj.core.api.SoftAssertions();
        table.asMap(String.class, String.class)
                .forEach((path, value) -> soft.assertThat(ctx.mxMessage.get(String.valueOf(path))).as(String.valueOf(path)).isEqualTo(value));
        soft.assertAll();
    }

    @Then("the MX UETR should equal the MT block 3 UETR")
    public void uetrPreserved() {
        Assertions.assertThat(ctx.mxMessage.get("CdtTrfTxInf/PmtId/UETR")).isEqualTo(ctx.mtMessage.getUetr());
    }
}
