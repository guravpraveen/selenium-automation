package com.payments.automation.swift;

import java.util.List;

/** Builds FIN MT messages (MT103, MT202, MT202 COV) from a {@link PaymentInstruction}. */
public final class MtBuilder {

    private MtBuilder() {
    }

    /** MT103 Single Customer Credit Transfer. */
    public static MtMessage mt103(PaymentInstruction p) {
        MtMessage m = headers(p, "103");
        m.addField("20", p.getReference());
        m.addField("23B", p.getBankOperationCode());
        m.addField("32A", SwiftValidators.isoToMtDate(p.getValueDate()) + p.getCurrency()
                + SwiftValidators.decimalToMtAmount(p.getAmount()));
        if (p.getInstructedAmount() != null) {
            m.addField("33B", p.getInstructedCurrency() + SwiftValidators.decimalToMtAmount(p.getInstructedAmount()));
        }
        m.addField("50K", party(p.getDebtorAccount(), p.getDebtorName(), p.getDebtorAddress()));
        if (p.getDebtorAgentBic() != null) {
            m.addField("52A", p.getDebtorAgentBic());
        }
        if (p.getIntermediaryBic() != null) {
            m.addField("56A", p.getIntermediaryBic());
        }
        if (p.getCreditorAgentBic() != null) {
            m.addField("57A", p.getCreditorAgentBic());
        }
        m.addField("59", party(p.getCreditorAccount(), p.getCreditorName(), p.getCreditorAddress()));
        if (p.getRemittanceInfo() != null) {
            m.addField("70", wrap(p.getRemittanceInfo(), 35, 4));
        }
        m.addField("71A", p.getCharges().name());
        if (p.getCharges() == PaymentInstruction.Charges.BEN) {
            m.addField("71F", p.getCurrency() + "0,");
        }
        return m;
    }

    /** MT202 General Financial Institution Transfer (bank-to-bank, e.g. liquidity / nostro funding). */
    public static MtMessage mt202(PaymentInstruction p) {
        MtMessage m = headers(p, "202");
        m.addField("20", p.getReference());
        m.addField("21", p.getRelatedReference() == null ? "NONREF" : p.getRelatedReference());
        m.addField("32A", SwiftValidators.isoToMtDate(p.getValueDate()) + p.getCurrency()
                + SwiftValidators.decimalToMtAmount(p.getAmount()));
        if (p.getDebtorAgentBic() != null) {
            m.addField("52A", p.getDebtorAgentBic());
        }
        if (p.getIntermediaryBic() != null) {
            m.addField("56A", p.getIntermediaryBic());
        }
        if (p.getCreditorAgentBic() != null) {
            m.addField("57A", p.getCreditorAgentBic());
        }
        m.addField("58A", p.getCreditorAccount() == null ? p.getReceiverBic()
                : "/" + p.getCreditorAccount() + "\n" + p.getReceiverBic());
        return m;
    }

    /** MT202 COV: block 3 {119:COV} plus sequence B with underlying customer details. */
    public static MtMessage mt202Cov(PaymentInstruction p) {
        MtMessage m = mt202(p);
        m.getUserHeader().put("119", "COV");
        m.addField("50K", party(p.getDebtorAccount(), p.getDebtorName(), p.getDebtorAddress()));
        m.addField("59", party(p.getCreditorAccount(), p.getCreditorName(), p.getCreditorAddress()));
        if (p.getRemittanceInfo() != null) {
            m.addField("70", wrap(p.getRemittanceInfo(), 35, 4));
        }
        return m;
    }

    private static MtMessage headers(PaymentInstruction p, String type) {
        MtMessage m = new MtMessage();
        m.setBasicHeader("F01" + ltAddress(p.getSenderBic()) + "0000000000");
        m.setApplicationHeader("I" + type + ltAddress(p.getReceiverBic()) + "N");
        if (p.getUetr() != null) {
            m.getUserHeader().put("121", p.getUetr());
        }
        return m;
    }

    /** BIC -> 12 char logical terminal address: BIC8 + 'A' + branch (XXX default). */
    static String ltAddress(String bic) {
        String bic8 = bic.substring(0, 8);
        String branch = bic.length() == 11 ? bic.substring(8) : "XXX";
        return bic8 + "A" + branch;
    }

    private static String party(String account, String name, List<String> address) {
        StringBuilder sb = new StringBuilder();
        if (account != null && !account.isEmpty()) {
            sb.append('/').append(account).append('\n');
        }
        sb.append(truncate(name, 35));
        for (int i = 0; i < Math.min(address.size(), 3); i++) {
            sb.append('\n').append(truncate(address.get(i), 35));
        }
        return sb.toString();
    }

    /** Splits free text into lines of {@code width} chars, max {@code lines} lines (e.g. :70: is 4*35x). */
    static String wrap(String text, int width, int lines) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < text.length() && count < lines; i += width, count++) {
            if (count > 0) {
                sb.append('\n');
            }
            sb.append(text, i, Math.min(text.length(), i + width));
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max);
    }
}
