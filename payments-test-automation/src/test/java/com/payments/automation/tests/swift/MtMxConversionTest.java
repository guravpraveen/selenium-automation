package com.payments.automation.tests.swift;

import com.payments.automation.data.DataProviders;
import com.payments.automation.reports.ExtentTestManager;
import com.payments.automation.swift.MtBuilder;
import com.payments.automation.swift.MtMessage;
import com.payments.automation.swift.MtMxConverter;
import com.payments.automation.swift.MtParser;
import com.payments.automation.swift.MtValidator;
import com.payments.automation.swift.MxBuilder;
import com.payments.automation.swift.MxMessage;
import com.payments.automation.swift.MxValidator;
import com.payments.automation.swift.PaymentInstruction;
import com.payments.automation.swift.PaymentInstruction.Scheme;
import com.payments.automation.utils.FileUtils;
import com.payments.automation.utils.TestDataGenerator;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Map;

/** MT -> MX migration checks (the core of CBPR+ / RTGS ISO 20022 regression). */
public class MtMxConversionTest {

    @Test(groups = {"swift", "smoke"}, description = "MT103 -> pacs.008 field mapping")
    public void mt103ToPacs008Mapping() {
        String fin = FileUtils.readResource("swift/mt/mt103_valid.txt");
        MtMessage mt = MtParser.parse(fin);
        String xml = MtMxConverter.mt103ToPacs008(fin);
        ExtentTestManager.xml("Translated pacs.008", xml);
        MxMessage mx = MxMessage.parse(xml);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(mx.get("CdtTrfTxInf/PmtId/InstrId")).as(":20: -> InstrId").isEqualTo(mt.getFieldOrEmpty("20"));
        soft.assertThat(mx.get("CdtTrfTxInf/PmtId/UETR")).as("{121:} -> UETR").isEqualTo(mt.getUetr());
        soft.assertThat(mx.get("CdtTrfTxInf/IntrBkSttlmDt")).as(":32A: date").isEqualTo("2026-10-17");
        soft.assertThat(mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy")).as(":32A: ccy").isEqualTo("USD");
        soft.assertThat(mx.get("CdtTrfTxInf/IntrBkSttlmAmt")).as(":32A: amount").isEqualTo("25000.00");
        soft.assertThat(mx.get("CdtTrfTxInf/ChrgBr")).as(":71A: SHA -> SHAR").isEqualTo("SHAR");
        soft.assertThat(mx.get("CdtTrfTxInf/Dbtr/Nm")).as(":50K: name").isEqualTo("ACME TRADING LTD");
        soft.assertThat(mx.get("CdtTrfTxInf/DbtrAcct/Id/IBAN")).as(":50K: IBAN").isEqualTo("GB29NWBK60161331926819");
        soft.assertThat(mx.get("CdtTrfTxInf/Cdtr/Nm")).as(":59: name").isEqualTo("LION EXPORTS PTE LTD");
        soft.assertThat(mx.get("CdtTrfTxInf/CdtrAcct/Id/Othr/Id")).as(":59: account").isEqualTo("SG1234567890");
        soft.assertThat(mx.get("CdtTrfTxInf/RmtInf/Ustrd")).as(":70:").isEqualTo("INVOICE 2026-0098 OCT SHIPMENT");
        soft.assertThat(mx.get("AppHdr/MsgDefIdr")).isEqualTo("pacs.008.001.08");
        soft.assertAll();

        Assertions.assertThat(MxValidator.validate(mx, Scheme.CBPR_PLUS).getErrors()).isEmpty();
    }

    @Test(groups = "swift", description = "pacs.008 -> MT103 reverse translation")
    public void pacs008ToMt103() {
        MtMessage mt = MtMxConverter.pacs008ToMt103(FileUtils.readResource("swift/mx/pacs008_valid.xml"));
        ExtentTestManager.code("Translated MT103", mt.toFin());
        Assertions.assertThat(mt.getFieldOrEmpty("32A")).isEqualTo("261018HKD125000,5");
        Assertions.assertThat(mt.getFieldOrEmpty("71A")).isEqualTo("OUR");
        Assertions.assertThat(mt.getUetr()).isEqualTo("b7c8d9e0-1f2a-4b3c-8d4e-5f6a7b8c9d0e");
        Assertions.assertThat(MtValidator.validate(MtParser.parse(mt.toFin())).getErrors()).isEmpty();
    }

    @Test(groups = "swift", dataProvider = "mt202Data", dataProviderClass = DataProviders.class,
            description = "MT202 -> pacs.009 for RTGS and CBPR+ from Excel")
    public void mt202ToPacs009FromExcel(Map<String, String> row) {
        Scheme scheme = Scheme.valueOf(row.get("Scheme"));
        PaymentInstruction p = new PaymentInstruction()
                .scheme(scheme)
                .clearingSystem(row.get("ClearingSystem").isEmpty() ? null : row.get("ClearingSystem"))
                .reference(row.get("Reference"))
                .relatedReference(row.get("RelatedRef"))
                .uetr(TestDataGenerator.uetr())
                .senderBic(row.get("SenderBIC"))
                .receiverBic(row.get("ReceiverBIC"))
                .currency(row.get("Currency"))
                .amount(row.get("Amount"))
                .valueDate(LocalDate.now())
                .creditorAccount(row.get("CreditorAccount"));

        MtMessage mt202 = MtBuilder.mt202(p);
        Assertions.assertThat(MtValidator.validate(MtParser.parse(mt202.toFin())).getErrors()).isEmpty();

        // MT202 has no scheme info, so the scheme / clearing system come from routing configuration
        PaymentInstruction canonical = MtMxConverter.fromMt(MtParser.parse(mt202.toFin()))
                .scheme(scheme).clearingSystem(p.getClearingSystem());
        String xml = MxBuilder.pacs009(canonical);
        ExtentTestManager.xml("pacs.009 (" + scheme + ")", xml);
        MxMessage mx = MxMessage.parse(xml);

        Assertions.assertThat(mx.getMessageType()).isEqualTo("pacs.009");
        Assertions.assertThat(mx.get("GrpHdr/SttlmInf/SttlmMtd")).isEqualTo(scheme == Scheme.RTGS ? "CLRG" : "INDA");
        Assertions.assertThat(MxValidator.validate(mx, scheme).getErrors()).isEmpty();
        Assertions.assertThat(MtMxConverter.fromMx(mx).getAmount()).isEqualByComparingTo(p.getAmount());
    }

    @Test(groups = "swift", description = "MT -> MX -> MT round trip keeps business data")
    public void roundTrip() {
        PaymentInstruction original = MtMxConverter.fromMt(MtParser.parse(FileUtils.readResource("swift/mt/mt103_valid.txt")));
        PaymentInstruction back = MtMxConverter.fromMx(MxMessage.parse(MxBuilder.pacs008(original)));

        Assertions.assertThat(back)
                .usingRecursiveComparison()
                .ignoringFields("messageId", "endToEndId", "scheme", "bankOperationCode", "clearingSystem")
                .withComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .isEqualTo(original);
    }
}
