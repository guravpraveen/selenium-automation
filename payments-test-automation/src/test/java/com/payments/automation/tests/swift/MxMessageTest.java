package com.payments.automation.tests.swift;

import com.payments.automation.reports.ExtentTestManager;
import com.payments.automation.swift.MxMessage;
import com.payments.automation.swift.MxValidator;
import com.payments.automation.swift.PaymentInstruction.Scheme;
import com.payments.automation.swift.ValidationResult;
import com.payments.automation.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class MxMessageTest {

    private MxMessage sample() {
        return MxMessage.parse(FileUtils.readResource("swift/mx/pacs008_valid.xml"));
    }

    @Test(groups = {"swift", "smoke"})
    public void readPacs008Fields() {
        MxMessage mx = sample();
        Assertions.assertThat(mx.getMessageDefinition()).isEqualTo("pacs.008.001.08");
        Assertions.assertThat(mx.get("GrpHdr/MsgId")).isEqualTo("TRN0003");
        Assertions.assertThat(mx.get("CdtTrfTxInf/IntrBkSttlmAmt")).isEqualTo("125000.50");
        Assertions.assertThat(mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy")).isEqualTo("HKD");
        Assertions.assertThat(mx.get("AppHdr/Fr/FIId/FinInstnId/BICFI")).isEqualTo("PAYBHKHHXXX");
        Assertions.assertThat(mx.getAll("Cdtr/PstlAdr/AdrLine")).containsExactly("1 HARBOUR ROAD", "KOWLOON HK");
    }

    @Test(groups = "swift")
    public void validCbprPacs008() {
        ValidationResult r = MxValidator.validate(sample(), Scheme.CBPR_PLUS);
        Assertions.assertThat(r.getErrors()).isEmpty();
    }

    @Test(groups = "swift", description = "Same CBPR+ message is rejected under RTGS rules (needs CLRG + ClrSys)")
    public void cbprMessageFailsRtgsRules() {
        ValidationResult r = MxValidator.validate(sample(), Scheme.RTGS);
        Assertions.assertThat(r.hasError("GH03")).isTrue();
        Assertions.assertThat(r.hasError("GH04")).isTrue();
    }

    @Test(groups = "swift", description = "Negative tests by mutating the XML")
    public void mutatedMessageErrors() {
        MxMessage mx = sample()
                .set("CdtTrfTxInf/PmtId/UETR", "not-a-uuid")
                .set("CdtTrfTxInf/IntrBkSttlmAmt", "100.123")       // 3 decimals for HKD
                .set("CdtTrfTxInf/ChrgBr", "OUR")                   // MT code, not MX
                .set("CdtTrfTxInf/InstdAgt/FinInstnId/BICFI", "BAD")
                .remove("CdtTrfTxInf/Cdtr/Nm");
        ValidationResult r = MxValidator.validate(MxMessage.parse(mx.toXml()), Scheme.CBPR_PLUS);
        ExtentTestManager.code("Errors", String.join("\n", r.getErrors()));
        Assertions.assertThat(r.hasError("TX01")).isTrue();
        Assertions.assertThat(r.hasError("TX05")).isTrue();
        Assertions.assertThat(r.hasError("TX07")).isTrue();
        Assertions.assertThat(r.hasError("AG01")).isTrue();
        Assertions.assertThat(r.hasError("PT02")).isTrue();
    }

    @Test(groups = "swift")
    public void xxeIsBlocked() {
        String evil = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>";
        Assertions.assertThatThrownBy(() -> MxMessage.parse(evil))
                .hasMessageContaining("Not well-formed");
    }
}
