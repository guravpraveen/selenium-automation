package com.payments.automation.swift;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Business-rule checks for pacs.008 / pacs.009 under CBPR+ or an RTGS scheme.
 * For full structural validation also call {@link MxMessage#validateAgainstXsd(String)}
 * with the official XSD / usage-guideline schema from MyStandards.
 */
public final class MxValidator {

    private static final Set<String> CHRG_BR = Set.of("DEBT", "CRED", "SHAR");

    private MxValidator() {
    }

    public static ValidationResult validate(MxMessage mx, PaymentInstruction.Scheme scheme) {
        ValidationResult r = new ValidationResult();
        String type = mx.getMessageType();
        r.check(type.equals("pacs.008") || type.equals("pacs.009"), "MX00", "Unsupported message " + type);

        // Business Application Header
        r.check(mx.exists("AppHdr"), "HDR1", "AppHdr (head.001) missing");
        if (mx.exists("AppHdr")) {
            r.check(SwiftValidators.isValidBic(mx.get("AppHdr/Fr/FIId/FinInstnId/BICFI")), "HDR2", "AppHdr From BIC invalid");
            r.check(SwiftValidators.isValidBic(mx.get("AppHdr/To/FIId/FinInstnId/BICFI")), "HDR3", "AppHdr To BIC invalid");
            r.check(mx.getMessageDefinition().equals(mx.get("AppHdr/MsgDefIdr")), "HDR4",
                    "AppHdr MsgDefIdr does not match Document namespace");
            r.check(String.valueOf(mx.get("GrpHdr/MsgId")).equals(mx.get("AppHdr/BizMsgIdr")), "HDR5",
                    "AppHdr BizMsgIdr should equal GrpHdr/MsgId");
        }

        // Group header
        String msgId = mx.get("GrpHdr/MsgId");
        r.check(msgId != null && !msgId.isEmpty() && msgId.length() <= 35, "GH01", "MsgId mandatory, max 35");
        r.check("1".equals(mx.get("GrpHdr/NbOfTxs")), "GH02", "CBPR+/RTGS allow exactly one transaction (NbOfTxs=1)");
        String sttlmMtd = mx.get("GrpHdr/SttlmInf/SttlmMtd");
        if (scheme == PaymentInstruction.Scheme.CBPR_PLUS) {
            r.check("INDA".equals(sttlmMtd) || "INGA".equals(sttlmMtd), "GH03",
                    "CBPR+ settlement method must be INDA or INGA, was " + sttlmMtd);
        } else {
            r.check("CLRG".equals(sttlmMtd), "GH03", "RTGS settlement method must be CLRG, was " + sttlmMtd);
            r.check(mx.exists("GrpHdr/SttlmInf/ClrSys/Cd"), "GH04", "RTGS requires ClrSys/Cd");
        }

        // Transaction
        r.check(SwiftValidators.isValidUetr(mx.get("CdtTrfTxInf/PmtId/UETR")), "TX01", "UETR mandatory UUID v4");
        String instrId = mx.get("CdtTrfTxInf/PmtId/InstrId");
        r.check(instrId == null || instrId.length() <= 16, "TX02",
                "CBPR+ restricts InstrId to 16 chars (MT :20: compatibility)");
        String e2e = mx.get("CdtTrfTxInf/PmtId/EndToEndId");
        r.check(e2e != null && !e2e.isEmpty() && e2e.length() <= 35, "TX03", "EndToEndId mandatory, max 35");

        String ccy = mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy");
        String amt = mx.get("CdtTrfTxInf/IntrBkSttlmAmt");
        r.check(SwiftValidators.isValidCurrency(ccy), "TX04", "Invalid currency " + ccy);
        r.check(SwiftValidators.isValidMxAmount(amt, ccy), "TX05", "Invalid amount " + amt + " for " + ccy);
        r.check(isIsoDate(mx.get("CdtTrfTxInf/IntrBkSttlmDt")), "TX06", "IntrBkSttlmDt must be YYYY-MM-DD");

        for (String agent : new String[]{"InstgAgt", "InstdAgt", "DbtrAgt", "CdtrAgt", "IntrmyAgt1"}) {
            String path = "CdtTrfTxInf/" + agent + "/FinInstnId/BICFI";
            if (mx.exists(path)) {
                r.check(SwiftValidators.isValidBic(mx.get(path)), "AG01", agent + " BIC invalid: " + mx.get(path));
            }
        }

        if (type.equals("pacs.008")) {
            String chrgBr = mx.get("CdtTrfTxInf/ChrgBr");
            r.check(CHRG_BR.contains(chrgBr), "TX07", "ChrgBr must be DEBT/CRED/SHAR, was " + chrgBr);
            String dbtr = mx.get("CdtTrfTxInf/Dbtr/Nm");
            String cdtr = mx.get("CdtTrfTxInf/Cdtr/Nm");
            r.check(dbtr != null && !dbtr.isEmpty() && dbtr.length() <= 140, "PT01", "Dbtr/Nm mandatory, max 140");
            r.check(cdtr != null && !cdtr.isEmpty() && cdtr.length() <= 140, "PT02", "Cdtr/Nm mandatory, max 140");
            r.check(mx.count("CdtTrfTxInf/Dbtr/PstlAdr/AdrLine") <= 3, "PT03",
                    "CBPR+ allows max 3 unstructured AdrLine (structured address from Nov-2026)");
            String ustrd = mx.get("CdtTrfTxInf/RmtInf/Ustrd");
            r.check(ustrd == null || ustrd.length() <= 140, "RM01", "RmtInf/Ustrd max 140");
        }
        return r;
    }

    private static boolean isIsoDate(String v) {
        try {
            LocalDate.parse(v);
            return true;
        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }
}
