package com.payments.automation.data;

import com.payments.automation.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Re-creates src/test/resources/testdata/PaymentsTestData.xlsx.
 * Run once (or after editing the rows below); afterwards just edit the Excel file directly:
 * <pre>mvn -q test-compile exec:java -Dexec.mainClass=com.payments.automation.data.TestDataWorkbookGenerator -Dexec.classpathScope=test</pre>
 * {@link ExcelUtils#createWorkbook} creates single-sheet files; this class shows multi-sheet creation with POI.
 */
public final class TestDataWorkbookGenerator {

    private TestDataWorkbookGenerator() {
    }

    public static void main(String[] args) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle header = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            header.setFont(bold);

            sheet(wb, header, "Login",
                    List.of("TestCaseId", "Execute", "Username", "Password", "ExpectedResult", "ExpectedMessage"),
                    List.of(
                            List.of("TC_LOGIN_01", "Y", "tomsmith", "SuperSecretPassword!", "SUCCESS", "You logged into a secure area!"),
                            List.of("TC_LOGIN_02", "Y", "tomsmith", "wrongPassword", "FAILURE", "Your password is invalid!"),
                            List.of("TC_LOGIN_03", "Y", "invalidUser", "SuperSecretPassword!", "FAILURE", "Your username is invalid!"),
                            List.of("TC_LOGIN_04", "N", "", "", "FAILURE", "Your username is invalid!")));

            sheet(wb, header, "MT103",
                    List.of("TestCaseId", "Execute", "Scheme", "Reference", "SenderBIC", "ReceiverBIC", "Currency", "Amount",
                            "ValueDateOffset", "DebtorName", "DebtorAccount", "CreditorName", "CreditorAccount",
                            "Charges", "Remittance", "ExpectedValid", "ExpectedErrorCode"),
                    List.of(
                            List.of("TC_MT103_01", "Y", "CBPR_PLUS", "TRN103USD01", "PAYBGB2LXXX", "PAYBSGSGXXX", "USD", "25000.00", "0",
                                    "ACME TRADING LTD", "GB29NWBK60161331926819", "LION EXPORTS PTE LTD", "SG1234567890", "SHA", "INVOICE 98", "TRUE", ""),
                            List.of("TC_MT103_02", "Y", "CBPR_PLUS", "TRN103JPY01", "PAYBGB2LXXX", "PAYBJPJTXXX", "JPY", "1500000", "1",
                                    "ACME TRADING LTD", "GB29NWBK60161331926819", "SAKURA PARTS KK", "JP0012345", "OUR", "PO 5521", "TRUE", ""),
                            List.of("TC_MT103_03", "Y", "CBPR_PLUS", "TRN103KWD01", "PAYBAEADXXX", "PAYBKWKWXXX", "KWD", "1200.125", "0",
                                    "DESERT OIL LLC", "AE070331234567890123456", "GULF SUPPLY CO", "KW81CBKU0000000000001234560101", "BEN", "CONTRACT 7", "TRUE", ""),
                            List.of("TC_MT103_04", "Y", "CBPR_PLUS", "/BADREF", "PAYBGB2LXXX", "PAYBSGSGXXX", "USD", "100.00", "0",
                                    "ACME TRADING LTD", "GB29NWBK60161331926819", "LION EXPORTS PTE LTD", "SG1234567890", "SHA", "NEG TEST", "FALSE", "T26"),
                            List.of("TC_MT103_05", "Y", "CBPR_PLUS", "TRN103BIC01", "PAYBGB2LXXX", "BAD1", "USD", "100.00", "0",
                                    "ACME TRADING LTD", "GB29NWBK60161331926819", "LION EXPORTS PTE LTD", "SG1234567890", "SHA", "NEG TEST", "FALSE", "H50")));

            sheet(wb, header, "MT202",
                    List.of("TestCaseId", "Execute", "Scheme", "ClearingSystem", "Reference", "RelatedRef", "SenderBIC", "ReceiverBIC",
                            "Currency", "Amount", "CreditorAccount"),
                    List.of(
                            List.of("TC_MT202_01", "Y", "RTGS", "INRTGS", "LQ20261017IN01", "NONREF", "PAYBINBBXXX", "CNTRINBBXXX", "INR", "7500000.00", "IN00RTGS0001"),
                            List.of("TC_MT202_02", "Y", "RTGS", "TGT", "LQ20261017EU01", "NONREF", "PAYBDEFFXXX", "CNTRDEFFXXX", "EUR", "15000000.00", "DE00TARGET0001"),
                            List.of("TC_MT202_03", "Y", "CBPR_PLUS", "", "LQ20261017US01", "COVREF001", "PAYBGB2LXXX", "CORRUS33XXX", "USD", "50000000.00", "US-NOSTRO-77")));

            sheet(wb, header, "BIC_IBAN",
                    List.of("TestCaseId", "Execute", "Type", "Value", "ExpectedValid"),
                    List.of(
                            List.of("TC_BIC_01", "Y", "BIC", "PAYBGB2LXXX", "TRUE"),
                            List.of("TC_BIC_02", "Y", "BIC", "PAYBGB2L", "TRUE"),
                            List.of("TC_BIC_03", "Y", "BIC", "PAYB12LX", "FALSE"),
                            List.of("TC_BIC_04", "Y", "BIC", "paybgb2l", "FALSE"),
                            List.of("TC_IBAN_01", "Y", "IBAN", "GB29NWBK60161331926819", "TRUE"),
                            List.of("TC_IBAN_02", "Y", "IBAN", "DE89370400440532013000", "TRUE"),
                            List.of("TC_IBAN_03", "Y", "IBAN", "GB29NWBK60161331926818", "FALSE"),
                            List.of("TC_UETR_01", "Y", "UETR", "e6f1a2b3-1c2d-4e5f-8a9b-0c1d2e3f4a5b", "TRUE"),
                            List.of("TC_UETR_02", "Y", "UETR", "e6f1a2b3-1c2d-1e5f-8a9b-0c1d2e3f4a5b", "FALSE")));

            sheet(wb, header, "ApiPosts",
                    List.of("TestCaseId", "Execute", "UserId", "Title", "Body", "ExpectedStatus"),
                    List.of(
                            List.of("TC_API_01", "Y", "1", "Nostro top-up", "Fund USD nostro before cut-off", "201"),
                            List.of("TC_API_02", "Y", "2", "RTGS liquidity", "Intraday liquidity transfer INR", "201"),
                            List.of("TC_API_03", "Y", "3", "CBPR+ migration", "MT103 to pacs.008 regression", "201")));

            var file = DataProviders.WORKBOOK;
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                wb.write(out);
            }
            System.out.println("Created " + file.toAbsolutePath());
        }
    }

    private static void sheet(XSSFWorkbook wb, XSSFCellStyle style, String name, List<String> headers, List<List<String>> rows) {
        Sheet sheet = wb.createSheet(name);
        Row h = sheet.createRow(0);
        for (int c = 0; c < headers.size(); c++) {
            h.createCell(c).setCellValue(headers.get(c));
            h.getCell(c).setCellStyle(style);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows.get(r).size(); c++) {
                row.createCell(c).setCellValue(rows.get(r).get(c));
            }
        }
        for (int c = 0; c < headers.size(); c++) {
            sheet.autoSizeColumn(c);
        }
    }
}
