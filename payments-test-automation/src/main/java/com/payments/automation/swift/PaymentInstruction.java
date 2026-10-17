package com.payments.automation.swift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Canonical (format-neutral) payment used to build MT and MX messages and to map
 * between them. Think of it as the "golden" expected record a test compares against.
 */
public class PaymentInstruction {

    public enum Scheme { CBPR_PLUS, RTGS }

    /** MT 71A values; MX ChrgBr equivalents via {@link #mxChargeBearer()}. */
    public enum Charges { OUR, BEN, SHA }

    private Scheme scheme = Scheme.CBPR_PLUS;
    private String messageId;
    private String reference;            // MT :20: / MX InstrId
    private String endToEndId;           // MX EndToEndId (MT :70: /ROC/ or :121: fallback)
    private String relatedReference;     // MT :21: (MT202)
    private String uetr;                 // MT {121:} / MX UETR
    private LocalDate valueDate;         // :32A: date / IntrBkSttlmDt
    private String currency;
    private BigDecimal amount;
    private String instructedCurrency;   // :33B: / InstdAmt
    private BigDecimal instructedAmount;
    private String senderBic;            // block 1 / InstgAgt / AppHdr Fr
    private String receiverBic;          // block 2 / InstdAgt / AppHdr To
    private String debtorName;
    private List<String> debtorAddress = new ArrayList<>();
    private String debtorAccount;
    private String debtorAgentBic;       // :52A:
    private String intermediaryBic;      // :56A:
    private String creditorAgentBic;     // :57A:
    private String creditorName;
    private List<String> creditorAddress = new ArrayList<>();
    private String creditorAccount;
    private String remittanceInfo;       // :70: / RmtInf/Ustrd
    private Charges charges = Charges.SHA;
    private String bankOperationCode = "CRED"; // :23B:
    private String clearingSystem;       // RTGS: e.g. TGT (T2), CHAPS, MEPS, CNAPS ...

    public String mxChargeBearer() {
        return switch (charges) {
            case OUR -> "DEBT";
            case BEN -> "CRED";
            case SHA -> "SHAR";
        };
    }

    public static Charges fromMxChargeBearer(String chrgBr) {
        return switch (chrgBr) {
            case "DEBT" -> Charges.OUR;
            case "CRED" -> Charges.BEN;
            default -> Charges.SHA;
        };
    }

    // ------------------------------------------------------------------ fluent setters / getters
    public Scheme getScheme() { return scheme; }
    public PaymentInstruction scheme(Scheme v) { scheme = v; return this; }
    public String getMessageId() { return messageId; }
    public PaymentInstruction messageId(String v) { messageId = v; return this; }
    public String getReference() { return reference; }
    public PaymentInstruction reference(String v) { reference = v; return this; }
    public String getEndToEndId() { return endToEndId; }
    public PaymentInstruction endToEndId(String v) { endToEndId = v; return this; }
    public String getRelatedReference() { return relatedReference; }
    public PaymentInstruction relatedReference(String v) { relatedReference = v; return this; }
    public String getUetr() { return uetr; }
    public PaymentInstruction uetr(String v) { uetr = v; return this; }
    public LocalDate getValueDate() { return valueDate; }
    public PaymentInstruction valueDate(LocalDate v) { valueDate = v; return this; }
    public String getCurrency() { return currency; }
    public PaymentInstruction currency(String v) { currency = v; return this; }
    public BigDecimal getAmount() { return amount; }
    public PaymentInstruction amount(BigDecimal v) { amount = v; return this; }
    public PaymentInstruction amount(String v) { amount = new BigDecimal(v); return this; }
    public String getInstructedCurrency() { return instructedCurrency; }
    public PaymentInstruction instructedCurrency(String v) { instructedCurrency = v; return this; }
    public BigDecimal getInstructedAmount() { return instructedAmount; }
    public PaymentInstruction instructedAmount(BigDecimal v) { instructedAmount = v; return this; }
    public String getSenderBic() { return senderBic; }
    public PaymentInstruction senderBic(String v) { senderBic = v; return this; }
    public String getReceiverBic() { return receiverBic; }
    public PaymentInstruction receiverBic(String v) { receiverBic = v; return this; }
    public String getDebtorName() { return debtorName; }
    public PaymentInstruction debtorName(String v) { debtorName = v; return this; }
    public List<String> getDebtorAddress() { return debtorAddress; }
    public PaymentInstruction debtorAddress(List<String> v) { debtorAddress = new ArrayList<>(v); return this; }
    public String getDebtorAccount() { return debtorAccount; }
    public PaymentInstruction debtorAccount(String v) { debtorAccount = v; return this; }
    public String getDebtorAgentBic() { return debtorAgentBic; }
    public PaymentInstruction debtorAgentBic(String v) { debtorAgentBic = v; return this; }
    public String getIntermediaryBic() { return intermediaryBic; }
    public PaymentInstruction intermediaryBic(String v) { intermediaryBic = v; return this; }
    public String getCreditorAgentBic() { return creditorAgentBic; }
    public PaymentInstruction creditorAgentBic(String v) { creditorAgentBic = v; return this; }
    public String getCreditorName() { return creditorName; }
    public PaymentInstruction creditorName(String v) { creditorName = v; return this; }
    public List<String> getCreditorAddress() { return creditorAddress; }
    public PaymentInstruction creditorAddress(List<String> v) { creditorAddress = new ArrayList<>(v); return this; }
    public String getCreditorAccount() { return creditorAccount; }
    public PaymentInstruction creditorAccount(String v) { creditorAccount = v; return this; }
    public String getRemittanceInfo() { return remittanceInfo; }
    public PaymentInstruction remittanceInfo(String v) { remittanceInfo = v; return this; }
    public Charges getCharges() { return charges; }
    public PaymentInstruction charges(Charges v) { charges = v; return this; }
    public String getBankOperationCode() { return bankOperationCode; }
    public PaymentInstruction bankOperationCode(String v) { bankOperationCode = v; return this; }
    public String getClearingSystem() { return clearingSystem; }
    public PaymentInstruction clearingSystem(String v) { clearingSystem = v; return this; }

    @Override
    public String toString() {
        return "PaymentInstruction[" + scheme + " ref=" + reference + " uetr=" + uetr + " " + currency + " " + amount
                + " " + valueDate + " " + senderBic + "->" + receiverBic + "]";
    }
}
