# Payments Test Automation Framework

A hybrid **Selenium 4 + Appium + RestAssured + Cucumber BDD + TestNG** framework in Java 21,
built for **payments testing**: SWIFT **MT** and ISO 20022 **MX** messages (CBPR+ and RTGS),
Liquidity Management data checks on **Oracle** and **SAP HANA**, and the usual UI and API layers.

Everything except the real Oracle and HANA connections runs out of the box. The in-memory H2 database
and the public practice sites mean you can start practising right after `git clone`.

---

## 1. Tech stack (latest stable versions)

| Area | Library | Version |
|---|---|---|
| Language | Java | 21 |
| UI automation | Selenium WebDriver (with Selenium Manager, so no driver downloads) | 4.51.0 |
| Mobile web | Appium java-client (Android Chrome / iOS Safari) | 11.0.0 |
| Test runner | TestNG | 7.12.0 |
| BDD | Cucumber JVM + PicoContainer (Gherkin) | 8.0.4 |
| API | RestAssured + JSON Schema Validator | 6.2.0 |
| Oracle | ojdbc17 | 23.26.3 |
| SAP HANA | ngdbc | 2.30.11 |
| Practice DB | H2 (Oracle mode) | 2.5.252 |
| Excel | Apache POI | 5.5.1 |
| Reports | ExtentReports Spark, Cucumber HTML, TestNG HTML | 5.1.2 |
| Assertions | AssertJ (fluent + soft assertions) | 3.27.7 |
| Test data | Datafaker | 2.7.0 |
| Logging | Log4j2 | 2.26.1 |

---

## 2. Project structure

```
payments-test-automation
├── pom.xml
├── testng-suites/                 regression, smoke, ui, cross-browser, mobile, tablet, api, db, swift, cucumber, offline
├── TABLET_SETUP.md                running on a Lenovo Idea Tab / any Android device
├── docker-compose-grid.yml        Selenium Grid 4 with Chrome, Firefox and Edge
└── src
    ├── main/java/com/payments/automation
    │   ├── config/ConfigReader            properties, then env, then -D overrides, plus ${ENV_VAR} placeholders
    │   ├── drivers/                       DriverFactory (Chrome/Firefox/Edge/Safari/mobile/Appium/Grid), ThreadLocal DriverManager
    │   ├── base/BasePage                  ~100 wrapped WebDriver methods (see section 5)
    │   ├── base/BaseTest                  TestNG browser setup and teardown
    │   ├── utils/                         ExcelUtils, WaitUtils, ScreenshotUtils, JsonUtils, FileUtils, TestDataGenerator
    │   ├── db/                            DatabaseClient (common JDBC)
    │   │   ├── OracleDatabaseClient       Oracle-specific SQL  (separate file)
    │   │   ├── SapHanaDatabaseClient      SAP HANA-specific SQL (separate file)
    │   │   ├── H2DatabaseClient           offline practice DB
    │   │   └── DatabaseFactory            chooses the DB per application
    │   ├── api/                           ApiClient (all HTTP verbs), ResponseValidator, TokenManager (OAuth2), Extent filter
    │   ├── swift/                         MT parser/builder/validator, MX builder/validator, MT<->MX converter
    │   └── reports/                       ExtentManager, TestListener, RetryAnalyzer
    └── test
        ├── java/com/payments/automation
        │   ├── pages/                     Page Objects (LoginPage, SecureAreaPage, PracticePage)
        │   ├── tests/{ui,api,db,swift}/   TestNG tests
        │   ├── stepdefs/                  Cucumber steps: UI, API, DB, SWIFT
        │   ├── hooks/Hooks                browser + Extent + screenshot hooks
        │   ├── runners/                   CucumberTestRunner, FailedScenariosRunner
        │   ├── context/TestContext        shared scenario state (PicoContainer DI)
        │   └── data/                      DataProviders (Excel), TestDataWorkbookGenerator
        └── resources
            ├── config/                    config.properties, qa/uat.properties, db.properties
            ├── features/{ui,api,db,swift} Gherkin feature files
            ├── testdata/PaymentsTestData.xlsx   sheets: Login, MT103, MT202, BIC_IBAN, ApiPosts
            ├── swift/mt, swift/mx         sample MT103 / MT202 / pacs.008 messages
            ├── schemas/                   JSON schemas
            └── db/h2-schema.sql           PAYMENTS, PAYMENT_AUDIT, LIQUIDITY_POSITIONS
```

---

## 3. Getting started

Prerequisites: JDK 21, Maven 3.9 or later, and Chrome, Firefox or Edge. You don't need to download drivers;
Selenium Manager handles that.

```bash
cd payments-test-automation
mvn clean test                                   # full regression (UI + API + DB + SWIFT)
mvn test -Dsuite=offline                         # SWIFT + DB only, no browser and no internet
mvn test -Dsuite=smoke
mvn test -Dsuite=api
mvn test -Dsuite=swift
mvn test -Dsuite=db
mvn test -Dsuite=cucumber                        # all Gherkin scenarios
mvn test -Dsuite=cucumber -Dcucumber.filter.tags="@swift and @negative"
mvn test -Dsuite=cross-browser                   # Chrome + Firefox + Edge in parallel
mvn test -Dsuite=mobile                          # Chrome mobile emulation (Pixel 7)
mvn test -Dbrowser=firefox -Dheadless=true -Denv=uat
```

Selenium Grid:

```bash
docker compose -f docker-compose-grid.yml up -d
mvn test -Dsuite=cross-browser -Dgrid.url=http://localhost:4444
```

Real Android device, for example a **Lenovo Idea Tab** tablet: connect it with USB debugging on, start Appium, then run
`mvn test -Dsuite=tablet`. The full step-by-step guide is in **[TABLET_SETUP.md](TABLET_SETUP.md)**. It also explains
how to run the non-browser suites directly on the tablet with Termux.

---

## 4. Configuration

You can override any key in `config.properties` with `-Dkey=value`. Secrets use `${ENV_VAR}`
placeholders, so they never get committed:

```properties
oracle.password=${ORACLE_DB_PASSWORD}
hana.password=${HANA_DB_PASSWORD}
```

### Choosing the database per application

```properties
db.type=h2                     # default
app.liquidity.db=oracle        # Liquidity Management  -> Oracle
app.cbpr.db=hana               # CBPR+ payments hub    -> SAP HANA
app.rtgs.db=oracle.rtgs        # second Oracle connection (keys oracle.rtgs.*)
app.practice.db=h2
```

```java
DatabaseClient lm   = DatabaseFactory.forApplication("liquidity"); // OracleDatabaseClient
DatabaseClient cbpr = DatabaseFactory.forApplication("cbpr");      // SapHanaDatabaseClient
DatabaseClient def  = DatabaseFactory.getDefault();                // from db.type / -Ddb.type
```

In Gherkin: `Given I am connected to the "liquidity" application database`

| Method (all DBs) | Purpose |
|---|---|
| `executeQuery`, `queryForRow`, `querySingleValue`, `queryForString`, `queryColumn` | SELECT |
| `executeUpdate`, `executeBatch`, `executeScript`, `inTransaction` | DML/DDL with rollback |
| `callProcedure`, `callProcedureWithOut` | Stored procedures |
| `count`, `exists`, `getColumnNames` | Quick checks |
| `waitForValue` | Poll until an asynchronous status changes (RECEIVED, then VALIDATED, then SETTLED) |
| `tableExists`, `currentSchema`, `setSchema`, `nextSequenceValue`, `limit`, `databaseTime` | Different syntax per DB (DUAL vs DUMMY, ROWNUM vs LIMIT ...) |

Oracle only: `callFunction`, `version`, `tableExists(owner, table)`.
HANA only: `viewExists`, `version`, `tableExists(schema, table)`.

---

## 5. Selenium: methods in `BasePage`

| Group | Methods |
|---|---|
| Navigation | `open`, `openPath`, `refresh`, `back`, `forward`, `getTitle`, `getCurrentUrl` |
| Actions | `click` (with JS fallback), `type`, `typeSecret`, `typeAndEnter`, `clearWithKeys`, `submit` |
| Reads | `getText`, `getTexts`, `getAttribute`, `getProperty`, `getValue`, `getCssValue`, `isDisplayed`, `isEnabled`, `isSelected`, `count` |
| Dropdowns | `selectByVisibleText`, `selectByValue`, `selectByIndex`, `selectByContainsText`, `getDropdownOptions`, `selectCustomDropdown` |
| Checkbox | `check`, `uncheck` |
| Actions API | `hover`, `doubleClick`, `rightClick`, `dragAndDrop`, `dragAndDropBy`, `clickAndHold`, `pressKey(s)` |
| JavaScript | `executeJs`, `jsClick`, `jsType`, `scrollIntoView`, `scrollToTop`, `scrollToBottom`, `scrollBy`, `highlight` |
| Alerts | `getAlertText`, `acceptAlert`, `dismissAlert`, `typeInAlert` |
| Frames | `switchToFrame` (by locator, index or name), `switchToParentFrame`, `switchToDefaultContent` |
| Windows | `switchToNewWindow`, `switchToWindowByTitle`, `openNewTab`, `openNewWindow`, `closeCurrentAndSwitchTo` |
| Cookies | `addCookie`, `getCookie`, `getAllCookies`, `deleteCookie`, `deleteAllCookies` |
| Tables | `readTable` (returns a List of Maps), `getCellText` |
| Selenium 4 | Relative locators `findBelow`, `findAbove`, `findToRightOf`, `findToLeftOf`, `findNear` |
| Evidence | `takeScreenshot`, `takeElementScreenshot` |

Waits are explicit (`WaitUtils`: visible, clickable, invisible, text, URL, alert, frame, windows,
page-load, AJAX, fluent wait). The implicit wait stays at 0 on purpose.

---

## 6. API: `ApiClient` (RestAssured)

```java
Response r = new ApiClient()
        .bearerToken(TokenManager.getToken())
        .header("X-Correlation-Id", uetr)
        .pathParam("uetr", uetr)
        .get("/payments/{uetr}/status");

ResponseValidator.of(r).statusCode(200).jsonValue("status", "ACSC").matchesSchema("schemas/status.json");
```

* Verbs: `get`, `post`, `put`, `patch`, `delete`, `head`, `options`, `send(Method, path)`, `download`
* Request: `header(s)`, `queryParam(s)`, `pathParam(s)`, `formParam(s)`, `cookie(s)`, `body` (POJO, Map, String or File), `xmlBody` (for MX), `multipart`
* Auth: `basicAuth`, `digestAuth`, `bearerToken`, `oauth2`, `apiKey`, plus `TokenManager` for OAuth2 client-credentials
* Validation: status, response time, headers, JSONPath, XMLPath, array size, JSON schema, deserialise to POJO
* Every request and response gets logged into the Extent report, with the Authorization header masked

---

## 7. SWIFT MT and ISO 20022 MX module

| Class | What it does |
|---|---|
| `MtParser` / `MtMessage` | Parse FIN blocks 1 to 5; read the sender/receiver BIC, UETR {121:} and fields (repeating and multi-line) |
| `MtBuilder` | Build MT103, MT202 and MT202 COV |
| `MtValidator` | Mandatory fields, :20: rules, :32A: date/currency/amount, :23B:, :71A: vs :71F:/:71G:, :33B:/:36:, BIC checks, X-charset, gpi UETR |
| `MxBuilder` | pacs.008.001.08 and pacs.009.001.08 with AppHdr head.001.001.02. CBPR+ uses INDA; RTGS uses CLRG plus ClrSys |
| `MxMessage` | Namespace-free XPath (`mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy")`), `set`/`remove` for negative tests, XSD validation, XXE-safe |
| `MxValidator` | CBPR+/RTGS rules: AppHdr consistency, NbOfTxs=1, settlement method, UETR, InstrId ≤ 16, amount decimals per ISO 4217, ChrgBr, BICs, name and address limits |
| `MtMxConverter` | MT103 ↔ pacs.008, MT202 ↔ pacs.009, round trip via a canonical `PaymentInstruction` |
| `SwiftValidators` | BIC, IBAN (mod-97), UETR v4, currency decimals (JPY 0, KWD 3 ...), MT↔MX amount and date conversion |

> For full schema validation, download the official XSDs / CBPR+ usage-guideline schemas from SWIFT
> MyStandards, put them in `src/test/resources/swift/xsd/`, and call `mx.validateAgainstXsd("swift/xsd/pacs.008.001.08.xsd")`.

All BICs in the sample data (`PAYB...`, `CORR...`, `CNTR...`) are made up.

---

## 8. Data-driven testing with Excel

`src/test/resources/testdata/PaymentsTestData.xlsx`. Row 1 holds the headers. Set the `Execute` column to `Y` or `N` to switch rows on and off.

```java
@Test(dataProvider = "mt103Data", dataProviderClass = DataProviders.class)
public void buildMt103FromExcel(Map<String, String> row) { ... row.get("Currency") ... }
```

`ExcelUtils` methods: `readSheet`, `readExecutableRows`, `readRow`, `getCellData`, `getRowCount`,
`setCellData`, `writeResult`, `createWorkbook`, `toDataProvider`.
To regenerate the workbook, run `TestDataWorkbookGenerator` (or just edit the file in Excel).

---

## 9. Reports

| Report | Location |
|---|---|
| Extent Spark (TestNG and Cucumber, with screenshots, API calls and MT/MX payloads) | `target/reports/ExtentReport_<timestamp>.html` |
| Cucumber HTML / JSON / JUnit XML | `target/reports/cucumber/` |
| TestNG HTML and emailable report | `target/surefire-reports/` |
| Logs | `target/logs/automation.log` |

Failed Cucumber scenarios get written to `target/reports/cucumber/rerun.txt`. Run `FailedScenariosRunner` to re-execute only those.
Retry for flaky tests: `-Dretry.count=1`.

---

## 10. Extras worth knowing

* **Selenium Manager**: no WebDriverManager and no driver binaries to maintain.
* **Chrome mobile emulation**: test responsive pages without a device (`-Dbrowser=chrome_mobile_emulation`).
* **Selenium 4 relative locators** and the `newWindow(TAB)` API.
* **ThreadLocal driver**: tests run in parallel safely (`cross-browser.xml` runs three browsers at once).
* **Soft assertions** (AssertJ): report every mismatched MT→MX field in one go.
* **GitHub Actions**: `.github/workflows/payments-tests.yml` runs regression and Cucumber headless and uploads the reports.
* **Selenium Grid in Docker**: `docker-compose-grid.yml`, with live view at `http://localhost:7900`.

---

## 11. Suggested daily practice plan

| Day | Exercise |
|---|---|
| 1 | Run `offline` and `smoke`, then open the Extent report and read each test |
| 2 | Add a page object for `/add_remove_elements` and use `count`, `click` and `findAll` |
| 3 | Add an `/upload` test using `uploadFile` and `takeElementScreenshot` |
| 4 | Write an API scenario in Gherkin for `GET /comments?postId=1` with no new Java code |
| 5 | Add a `MT103` Excel row with EUR and `:33B:` in GBP, and expect error `D75` (missing `:36:`) |
| 6 | Add an MT940 parser test (statement lines `:61:` / `:86:`), reusing `MtParser` |
| 7 | Build a `camt.053` or `pacs.002` (status report) builder alongside `MxBuilder` |
| 8 | Add a `PAYMENT_AUDIT` DB check after a status change, in Gherkin |
| 9 | Point `app.liquidity.db` at a real Oracle schema and reuse the same feature |
| 10 | Add RTGS country rules to `MxValidator` (for example a TARGET2 cut-off time or INR RTGS minimum amount) |
