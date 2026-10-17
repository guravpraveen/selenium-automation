package com.payments.automation.swift;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds CBPR+ / RTGS style ISO 20022 messages with Business Application Header (head.001.001.02).
 *
 * <ul>
 *   <li>pacs.008.001.08 - FI to FI Customer Credit Transfer   (MT103 equivalent)</li>
 *   <li>pacs.009.001.08 - Financial Institution Credit Transfer (MT202 equivalent)</li>
 * </ul>
 * CBPR+ uses settlement method INDA (correspondent accounts); RTGS (e.g. T2, CHAPS, MEPS+, FedNow-like
 * schemes) uses CLRG with a clearing system code. Output is wrapped in a {@code <BusMsg>} element
 * holding AppHdr + Document, the way most payment hubs store/transport the pair.
 */
public final class MxBuilder {

    public static final String NS_HEAD = "urn:iso:std:iso:20022:tech:xsd:head.001.001.02";
    public static final String NS_PACS008 = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";
    public static final String NS_PACS009 = "urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08";

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private MxBuilder() {
    }

    public static String pacs008(PaymentInstruction p) {
        String created = OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DT);
        String msgId = p.getMessageId() != null ? p.getMessageId() : p.getReference();
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<BusMsg>\n");
        appHdr(x, p, msgId, "pacs.008.001.08", created);
        x.append("<Document xmlns=\"").append(NS_PACS008).append("\">\n<FIToFICstmrCdtTrf>\n");
        grpHdr(x, p, msgId, created);
        x.append("<CdtTrfTxInf>\n");
        pmtId(x, p);
        x.append(el("IntrBkSttlmAmt", "Ccy", p.getCurrency(),
                SwiftValidators.decimalToMxAmount(p.getAmount(), p.getCurrency())));
        x.append(el("IntrBkSttlmDt", p.getValueDate().toString()));
        if (p.getInstructedAmount() != null) {
            x.append(el("InstdAmt", "Ccy", p.getInstructedCurrency(),
                    SwiftValidators.decimalToMxAmount(p.getInstructedAmount(), p.getInstructedCurrency())));
        }
        x.append(el("ChrgBr", p.mxChargeBearer()));
        agent(x, "InstgAgt", p.getSenderBic());
        agent(x, "InstdAgt", p.getReceiverBic());
        if (p.getIntermediaryBic() != null) {
            agent(x, "IntrmyAgt1", p.getIntermediaryBic());
        }
        party(x, "Dbtr", p.getDebtorName(), p.getDebtorAddress());
        account(x, "DbtrAcct", p.getDebtorAccount());
        agent(x, "DbtrAgt", p.getDebtorAgentBic() != null ? p.getDebtorAgentBic() : p.getSenderBic());
        agent(x, "CdtrAgt", p.getCreditorAgentBic() != null ? p.getCreditorAgentBic() : p.getReceiverBic());
        party(x, "Cdtr", p.getCreditorName(), p.getCreditorAddress());
        account(x, "CdtrAcct", p.getCreditorAccount());
        if (p.getRemittanceInfo() != null) {
            x.append("<RmtInf>").append(el("Ustrd", p.getRemittanceInfo())).append("</RmtInf>\n");
        }
        x.append("</CdtTrfTxInf>\n</FIToFICstmrCdtTrf>\n</Document>\n</BusMsg>\n");
        return x.toString();
    }

    public static String pacs009(PaymentInstruction p) {
        String created = OffsetDateTime.now(ZoneOffset.UTC).format(ISO_DT);
        String msgId = p.getMessageId() != null ? p.getMessageId() : p.getReference();
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<BusMsg>\n");
        appHdr(x, p, msgId, "pacs.009.001.08", created);
        x.append("<Document xmlns=\"").append(NS_PACS009).append("\">\n<FICdtTrf>\n");
        grpHdr(x, p, msgId, created);
        x.append("<CdtTrfTxInf>\n");
        pmtId(x, p);
        x.append(el("IntrBkSttlmAmt", "Ccy", p.getCurrency(),
                SwiftValidators.decimalToMxAmount(p.getAmount(), p.getCurrency())));
        x.append(el("IntrBkSttlmDt", p.getValueDate().toString()));
        agent(x, "InstgAgt", p.getSenderBic());
        agent(x, "InstdAgt", p.getReceiverBic());
        if (p.getIntermediaryBic() != null) {
            agent(x, "IntrmyAgt1", p.getIntermediaryBic());
        }
        agent(x, "Dbtr", p.getDebtorAgentBic() != null ? p.getDebtorAgentBic() : p.getSenderBic());
        if (p.getCreditorAgentBic() != null) {
            agent(x, "CdtrAgt", p.getCreditorAgentBic());
        }
        agent(x, "Cdtr", p.getReceiverBic());
        account(x, "CdtrAcct", p.getCreditorAccount());
        x.append("</CdtTrfTxInf>\n</FICdtTrf>\n</Document>\n</BusMsg>\n");
        return x.toString();
    }

    // ------------------------------------------------------------------ building blocks
    private static void appHdr(StringBuilder x, PaymentInstruction p, String msgId, String msgDef, String created) {
        x.append("<AppHdr xmlns=\"").append(NS_HEAD).append("\">\n");
        x.append("<Fr><FIId><FinInstnId>").append(el("BICFI", p.getSenderBic())).append("</FinInstnId></FIId></Fr>\n");
        x.append("<To><FIId><FinInstnId>").append(el("BICFI", p.getReceiverBic())).append("</FinInstnId></FIId></To>\n");
        x.append(el("BizMsgIdr", msgId));
        x.append(el("MsgDefIdr", msgDef));
        x.append(el("BizSvc", p.getScheme() == PaymentInstruction.Scheme.CBPR_PLUS ? "swift.cbprplus.02" : "rtgs.01"));
        x.append(el("CreDt", created));
        x.append("</AppHdr>\n");
    }

    private static void grpHdr(StringBuilder x, PaymentInstruction p, String msgId, String created) {
        x.append("<GrpHdr>\n");
        x.append(el("MsgId", msgId));
        x.append(el("CreDtTm", created));
        x.append(el("NbOfTxs", "1"));
        x.append("<SttlmInf>");
        if (p.getScheme() == PaymentInstruction.Scheme.RTGS) {
            x.append(el("SttlmMtd", "CLRG"));
            if (p.getClearingSystem() != null) {
                x.append("<ClrSys>").append(el("Cd", p.getClearingSystem())).append("</ClrSys>");
            }
        } else {
            x.append(el("SttlmMtd", "INDA"));
        }
        x.append("</SttlmInf>\n</GrpHdr>\n");
    }

    private static void pmtId(StringBuilder x, PaymentInstruction p) {
        x.append("<PmtId>");
        x.append(el("InstrId", p.getReference()));
        x.append(el("EndToEndId", p.getEndToEndId() != null ? p.getEndToEndId() : "NOTPROVIDED"));
        x.append(el("UETR", p.getUetr()));
        x.append("</PmtId>\n");
    }

    private static void agent(StringBuilder x, String tag, String bic) {
        if (bic != null) {
            x.append('<').append(tag).append("><FinInstnId>").append(el("BICFI", bic))
                    .append("</FinInstnId></").append(tag).append(">\n");
        }
    }

    private static void party(StringBuilder x, String tag, String name, List<String> address) {
        x.append('<').append(tag).append('>').append(el("Nm", name));
        if (address != null && !address.isEmpty()) {
            x.append("<PstlAdr>");
            for (String line : address.subList(0, Math.min(address.size(), 3))) {
                x.append(el("AdrLine", line));
            }
            x.append("</PstlAdr>");
        }
        x.append("</").append(tag).append(">\n");
    }

    private static void account(StringBuilder x, String tag, String account) {
        if (account == null || account.isEmpty()) {
            return;
        }
        x.append('<').append(tag).append("><Id>");
        if (SwiftValidators.isValidIban(account)) {
            x.append(el("IBAN", account));
        } else {
            x.append("<Othr>").append(el("Id", account)).append("</Othr>");
        }
        x.append("</Id></").append(tag).append(">\n");
    }

    private static String el(String tag, String value) {
        return "<" + tag + ">" + escape(value) + "</" + tag + ">";
    }

    private static String el(String tag, String attr, String attrValue, String value) {
        return "<" + tag + " " + attr + "=\"" + escape(attrValue) + "\">" + escape(value) + "</" + tag + ">\n";
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
