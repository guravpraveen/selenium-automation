package com.payments.automation.tests.swift;

import com.payments.automation.data.DataProviders;
import com.payments.automation.reports.ExtentTestManager;
import com.payments.automation.swift.MtBuilder;
import com.payments.automation.swift.MtMessage;
import com.payments.automation.swift.MtParser;
import com.payments.automation.swift.MtValidator;
import com.payments.automation.swift.PaymentInstruction;
import com.payments.automation.swift.ValidationResult;
import com.payments.automation.utils.FileUtils;
import com.payments.automation.utils.TestDataGenerator;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MtMessageTest {

    @Test(groups = {"swift", "smoke"}, description = "Parse an MT103 and read header + text block fields")
    public void parseMt103() {
        MtMessage mt = MtParser.parse(FileUtils.readResource("swift/mt/mt103_valid.txt"));
        Assertions.assertThat(mt.getMessageType()).isEqualTo("103");
        Assertions.assertThat(mt.getSenderBic()).isEqualTo("PAYBGB2L");
        Assertions.assertThat(mt.getReceiverBic()).isEqualTo("PAYBSGSG");
        Assertions.assertThat(mt.getUetr()).isEqualTo("e6f1a2b3-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
        Assertions.assertThat(mt.getFieldOrEmpty("20")).isEqualTo("TRN0001");
        Assertions.assertThat(mt.getFieldOrEmpty("32A")).isEqualTo("261017USD25000,");
        Assertions.assertThat(mt.getFieldOrEmpty("50K")).startsWith("/GB29NWBK60161331926819\nACME TRADING LTD");
        Assertions.assertThat(mt.getTrailer()).containsKey("CHK");
    }

    @Test(groups = "swift")
    public void validMt103PassesValidation() {
        ValidationResult r = MtValidator.validate(MtParser.parse(FileUtils.readResource("swift/mt/mt103_valid.txt")));
        Assertions.assertThat(r.getErrors()).isEmpty();
    }

    @Test(groups = "swift", description = "Negative MT103: every broken rule is reported")
    public void invalidMt103ReportsAllErrors() {
        ValidationResult r = MtValidator.validate(MtParser.parse(FileUtils.readResource("swift/mt/mt103_invalid.txt")));
        ExtentTestManager.code("Validation errors", String.join("\n", r.getErrors()));
        Assertions.assertThat(r.isValid()).isFalse();
        Assertions.assertThat(r.hasError("T26")).as(":20: slashes").isTrue();
        Assertions.assertThat(r.hasError("T36")).as(":23B: code").isTrue();
        Assertions.assertThat(r.hasError("T50")).as(":32A: date 261317").isTrue();
        Assertions.assertThat(r.hasError("C03")).as("JPY with decimals").isTrue();
        Assertions.assertThat(r.hasError("E13")).as("OUR with 71F").isTrue();
        Assertions.assertThat(r.hasError("G01")).as("missing UETR").isTrue();
    }

    @Test(groups = "swift")
    public void validMt202() {
        MtMessage mt = MtParser.parse(FileUtils.readResource("swift/mt/mt202_valid.txt"));
        Assertions.assertThat(mt.getMessageType()).isEqualTo("202");
        Assertions.assertThat(MtValidator.validate(mt).getErrors()).isEmpty();
    }

    @Test(groups = "swift", dataProvider = "mt103Data", dataProviderClass = DataProviders.class,
            description = "Build MT103 from Excel row, round-trip through parser and validate")
    public void buildMt103FromExcel(Map<String, String> row) {
        PaymentInstruction p = new PaymentInstruction()
                .reference(row.get("Reference"))
                .uetr(TestDataGenerator.uetr())
                .senderBic(row.get("SenderBIC"))
                .receiverBic(row.get("ReceiverBIC"))
                .currency(row.get("Currency"))
                .amount(row.get("Amount"))
                .valueDate(LocalDate.now().plusDays(Long.parseLong(row.get("ValueDateOffset"))))
                .debtorName(row.get("DebtorName"))
                .debtorAccount(row.get("DebtorAccount"))
                .debtorAddress(List.of("1 MAIN STREET", "LONDON GB"))
                .creditorName(row.get("CreditorName"))
                .creditorAccount(row.get("CreditorAccount"))
                .remittanceInfo(row.get("Remittance"))
                .charges(PaymentInstruction.Charges.valueOf(row.get("Charges")));

        String fin;
        try {
            fin = MtBuilder.mt103(p).toFin();
        } catch (RuntimeException e) {
            // e.g. invalid BIC too short to build an LT address - an expected negative outcome
            Assertions.assertThat(row.get("ExpectedValid")).isEqualTo("FALSE");
            return;
        }
        ExtentTestManager.code("Generated MT103", fin);
        ValidationResult r = MtValidator.validate(MtParser.parse(fin));
        ExtentTestManager.info("Validation: " + r);

        if (Boolean.parseBoolean(row.get("ExpectedValid"))) {
            Assertions.assertThat(r.getErrors()).isEmpty();
        } else {
            Assertions.assertThat(r.hasError(row.get("ExpectedErrorCode"))).as(r.toString()).isTrue();
        }
    }

    @Test(groups = "swift")
    public void mt202CovHasUnderlyingCustomer() {
        PaymentInstruction p = new PaymentInstruction()
                .reference("COV0001").relatedReference("TRN0001").uetr(TestDataGenerator.uetr())
                .senderBic("PAYBGB2LXXX").receiverBic("CORRUS33XXX")
                .currency("USD").amount("25000.00").valueDate(LocalDate.now())
                .debtorName("ACME TRADING LTD").debtorAccount("GB29NWBK60161331926819")
                .creditorName("LION EXPORTS PTE LTD").creditorAccount("SG1234567890");
        MtMessage cov = MtBuilder.mt202Cov(p);
        Assertions.assertThat(cov.getUserHeader()).containsEntry("119", "COV");
        Assertions.assertThat(MtValidator.validate(MtParser.parse(cov.toFin())).getErrors()).isEmpty();
    }
}
