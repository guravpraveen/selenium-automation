package com.payments.automation.tests.swift;

import com.payments.automation.data.DataProviders;
import com.payments.automation.swift.SwiftValidators;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class SwiftValidatorsTest {

    @Test(groups = "swift", dataProvider = "bicData", dataProviderClass = DataProviders.class)
    public void bicIbanUetrFromExcel(Map<String, String> row) {
        boolean actual = switch (row.get("Type")) {
            case "BIC" -> SwiftValidators.isValidBic(row.get("Value"));
            case "IBAN" -> SwiftValidators.isValidIban(row.get("Value"));
            case "UETR" -> SwiftValidators.isValidUetr(row.get("Value"));
            default -> throw new IllegalArgumentException(row.get("Type"));
        };
        Assertions.assertThat(actual).as(row.get("TestCaseId")).isEqualTo(Boolean.parseBoolean(row.get("ExpectedValid")));
    }

    @Test(groups = "swift")
    public void amountConversions() {
        Assertions.assertThat(SwiftValidators.decimalToMtAmount(new BigDecimal("1000.00"))).isEqualTo("1000,");
        Assertions.assertThat(SwiftValidators.decimalToMtAmount(new BigDecimal("1250.50"))).isEqualTo("1250,5");
        Assertions.assertThat(SwiftValidators.mtAmountToDecimal("1250,50")).isEqualByComparingTo("1250.50");
        Assertions.assertThat(SwiftValidators.decimalToMxAmount(new BigDecimal("1250.5"), "USD")).isEqualTo("1250.50");
        Assertions.assertThat(SwiftValidators.decimalToMxAmount(new BigDecimal("1500000"), "JPY")).isEqualTo("1500000");
        Assertions.assertThat(SwiftValidators.isValidMtAmount("100,123", "KWD")).isTrue();
        Assertions.assertThat(SwiftValidators.isValidMtAmount("100,1", "JPY")).isFalse();
        Assertions.assertThat(SwiftValidators.isValidMxAmount("100.12", "USD")).isTrue();
        Assertions.assertThat(SwiftValidators.isValidMxAmount("100,12", "USD")).isFalse();
    }

    @Test(groups = "swift")
    public void dateConversions() {
        Assertions.assertThat(SwiftValidators.mtDateToIso("261017")).isEqualTo(LocalDate.of(2026, 10, 17));
        Assertions.assertThat(SwiftValidators.isoToMtDate(LocalDate.of(2026, 1, 5))).isEqualTo("260105");
        Assertions.assertThat(SwiftValidators.isValidYyMmDd("261332")).isFalse();
    }
}
