package com.payments.automation.swift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MT <-> MX translation (simplified version of the SWIFT translation rules):
 * <ul>
 *   <li>MT103  <-> pacs.008</li>
 *   <li>MT202  <-> pacs.009</li>
 * </ul>
 * Everything goes through {@link PaymentInstruction}, so a test can compare
 * "source MT" and "translated MX" field by field on the canonical model.
 */
public final class MtMxConverter {

    private MtMxConverter() {
    }

    // ------------------------------------------------------------------ MT -> canonical
    public static PaymentInstruction fromMt(MtMessage mt) {
        PaymentInstruction p = new PaymentInstruction()
                .reference(mt.getFieldOrEmpty("20"))
                .uetr(mt.getUetr())
                .senderBic(mt.getSenderBic())
                .receiverBic(mt.getReceiverBic());

        String f32a = mt.getFieldOrEmpty("32A");
        p.valueDate(SwiftValidators.mtDateToIso(f32a.substring(0, 6)))
                .currency(f32a.substring(6, 9))
                .amount(SwiftValidators.mtAmountToDecimal(f32a.substring(9)));

        mt.getField("33B").ifPresent(v -> p.instructedCurrency(v.substring(0, 3))
                .instructedAmount(SwiftValidators.mtAmountToDecimal(v.substring(3))));
        mt.getField("21").ifPresent(p::relatedReference);
        mt.getField("23B").ifPresent(p::bankOperationCode);
        mt.getField("52A").ifPresent(p::debtorAgentBic);
        mt.getField("56A").ifPresent(p::intermediaryBic);
        mt.getField("57A").ifPresent(p::creditorAgentBic);
        mt.getField("70").ifPresent(v -> p.remittanceInfo(v.replace("\n", "")));
        mt.getField("71A").ifPresent(v -> p.charges(PaymentInstruction.Charges.valueOf(v)));

        mt.getFieldAnyOption("50").ifPresent(f -> {
            Party party = Party.parse(f.value());
            p.debtorAccount(party.account).debtorName(party.name).debtorAddress(party.address);
        });
        mt.getFieldAnyOption("59").ifPresent(f -> {
            Party party = Party.parse(f.value());
            p.creditorAccount(party.account).creditorName(party.name).creditorAddress(party.address);
        });
        mt.getField("58A").ifPresent(v -> {
            Party party = Party.parse(v);
            p.creditorAccount(party.account);
        });
        return p;
    }

    // ------------------------------------------------------------------ MX -> canonical
    public static PaymentInstruction fromMx(MxMessage mx) {
        String ccy = mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy");
        PaymentInstruction p = new PaymentInstruction()
                .scheme("CLRG".equals(mx.get("GrpHdr/SttlmInf/SttlmMtd"))
                        ? PaymentInstruction.Scheme.RTGS : PaymentInstruction.Scheme.CBPR_PLUS)
                .clearingSystem(mx.get("GrpHdr/SttlmInf/ClrSys/Cd"))
                .messageId(mx.get("GrpHdr/MsgId"))
                .reference(mx.get("CdtTrfTxInf/PmtId/InstrId"))
                .endToEndId(mx.get("CdtTrfTxInf/PmtId/EndToEndId"))
                .uetr(mx.get("CdtTrfTxInf/PmtId/UETR"))
                .currency(ccy)
                .amount(mx.get("CdtTrfTxInf/IntrBkSttlmAmt"))
                .valueDate(java.time.LocalDate.parse(mx.get("CdtTrfTxInf/IntrBkSttlmDt")))
                .senderBic(mx.get("CdtTrfTxInf/InstgAgt/FinInstnId/BICFI"))
                .receiverBic(mx.get("CdtTrfTxInf/InstdAgt/FinInstnId/BICFI"))
                .intermediaryBic(mx.get("CdtTrfTxInf/IntrmyAgt1/FinInstnId/BICFI"))
                .creditorAgentBic(mx.get("CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI"))
                .remittanceInfo(mx.get("CdtTrfTxInf/RmtInf/Ustrd"));

        if (mx.exists("CdtTrfTxInf/InstdAmt")) {
            p.instructedCurrency(mx.get("CdtTrfTxInf/InstdAmt/@Ccy"))
                    .instructedAmount(new java.math.BigDecimal(mx.get("CdtTrfTxInf/InstdAmt")));
        }
        if ("pacs.008".equals(mx.getMessageType())) {
            p.charges(PaymentInstruction.fromMxChargeBearer(mx.get("CdtTrfTxInf/ChrgBr")))
                    .debtorName(mx.get("CdtTrfTxInf/Dbtr/Nm"))
                    .debtorAddress(mx.getAll("CdtTrfTxInf/Dbtr/PstlAdr/AdrLine"))
                    .debtorAccount(account(mx, "DbtrAcct"))
                    .debtorAgentBic(mx.get("CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI"))
                    .creditorName(mx.get("CdtTrfTxInf/Cdtr/Nm"))
                    .creditorAddress(mx.getAll("CdtTrfTxInf/Cdtr/PstlAdr/AdrLine"))
                    .creditorAccount(account(mx, "CdtrAcct"));
        } else {
            p.debtorAgentBic(mx.get("CdtTrfTxInf/Dbtr/FinInstnId/BICFI"))
                    .creditorAccount(account(mx, "CdtrAcct"));
        }
        return p;
    }

    private static String account(MxMessage mx, String tag) {
        String iban = mx.get("CdtTrfTxInf/" + tag + "/Id/IBAN");
        return iban != null ? iban : mx.get("CdtTrfTxInf/" + tag + "/Id/Othr/Id");
    }

    // ------------------------------------------------------------------ direct translations
    public static String mt103ToPacs008(String finMt103) {
        MtMessage mt = MtParser.parse(finMt103);
        if (!"103".equals(mt.getMessageType())) {
            throw new SwiftValidationException("Expected MT103 but got MT" + mt.getMessageType());
        }
        return MxBuilder.pacs008(fromMt(mt));
    }

    public static String mt202ToPacs009(String finMt202) {
        MtMessage mt = MtParser.parse(finMt202);
        if (!"202".equals(mt.getMessageType())) {
            throw new SwiftValidationException("Expected MT202 but got MT" + mt.getMessageType());
        }
        return MxBuilder.pacs009(fromMt(mt));
    }

    public static MtMessage pacs008ToMt103(String xml) {
        return MtBuilder.mt103(fromMx(MxMessage.parse(xml)));
    }

    public static MtMessage pacs009ToMt202(String xml) {
        return MtBuilder.mt202(fromMx(MxMessage.parse(xml)));
    }

    /** Option K / no-letter party field: optional "/account" line, name line, up to 3 address lines. */
    private record Party(String account, String name, List<String> address) {
        static Party parse(String value) {
            List<String> lines = new ArrayList<>(Arrays.asList(value.split("\n")));
            String account = null;
            if (!lines.isEmpty() && lines.get(0).startsWith("/")) {
                account = lines.remove(0).substring(1);
            }
            String name = lines.isEmpty() ? null : lines.remove(0);
            return new Party(account, name, lines);
        }
    }
}
