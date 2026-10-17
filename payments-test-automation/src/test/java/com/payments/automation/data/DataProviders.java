package com.payments.automation.data;

import com.payments.automation.utils.ExcelUtils;
import org.testng.annotations.DataProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Central TestNG data providers backed by the Excel workbook in src/test/resources/testdata. */
public final class DataProviders {

    public static final Path WORKBOOK = Paths.get("src", "test", "resources", "testdata", "PaymentsTestData.xlsx");

    private DataProviders() {
    }

    @DataProvider(name = "loginData")
    public static Object[][] loginData() {
        return ExcelUtils.toDataProvider(ExcelUtils.readExecutableRows(WORKBOOK, "Login"));
    }

    @DataProvider(name = "mt103Data")
    public static Object[][] mt103Data() {
        return ExcelUtils.toDataProvider(ExcelUtils.readExecutableRows(WORKBOOK, "MT103"));
    }

    @DataProvider(name = "mt202Data")
    public static Object[][] mt202Data() {
        return ExcelUtils.toDataProvider(ExcelUtils.readExecutableRows(WORKBOOK, "MT202"));
    }

    @DataProvider(name = "bicData")
    public static Object[][] bicData() {
        return ExcelUtils.toDataProvider(ExcelUtils.readExecutableRows(WORKBOOK, "BIC_IBAN"));
    }

    @DataProvider(name = "apiPosts")
    public static Object[][] apiPosts() {
        return ExcelUtils.toDataProvider(ExcelUtils.readExecutableRows(WORKBOOK, "ApiPosts"));
    }
}
