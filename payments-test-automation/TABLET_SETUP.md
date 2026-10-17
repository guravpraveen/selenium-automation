# Running the tests on a Lenovo Idea Tab (Android tablet)

The Lenovo Idea Tab Plus runs **Android**, so there are two ways to use it:

| Option | What runs where | Which tests |
|---|---|---|
| **A. Tablet as the device under test** (recommended) | Framework runs on your Windows laptop. Appium drives **Chrome on the tablet** over USB or Wi-Fi | UI tests (TestNG + Cucumber `@ui`) in the tablet's Chrome |
| **B. Tablet as the computer** | Framework runs **on the tablet itself** inside Termux | SWIFT, DB, API and Cucumber non-UI (no browser tests) |

---

## Option A: Drive Chrome on the tablet from your laptop (Appium)

```
 Windows laptop                                          Lenovo Idea Tab
 ┌────────────────────────────┐   USB / Wi-Fi (adb)   ┌───────────────────┐
 │ mvn test -Dsuite=tablet    │ ───────────────────▶  │ Chrome browser    │
 │   ↓                        │                       │ (UiAutomator2 +   │
 │ Appium server :4723        │                       │  chromedriver)    │
 └────────────────────────────┘                       └───────────────────┘
```

### 1. Prepare the tablet (one-time)

1. **Turn on Developer options:** go to **Settings → About tablet** and tap **Build number** 7 times.
   On some Lenovo builds, Build number is under *About tablet → Software version / Software info*.
2. **Turn on USB debugging:** go to **Settings → System → Developer options** and switch on **USB debugging**.
3. In the same Developer options screen, switch on **Stay awake**, so the screen doesn't lock in the middle of a test.
4. Open **Chrome** once and accept the welcome screens, then update Chrome from the Play Store.

### 2. Prepare the Windows laptop (one-time)

| Install | Check it works |
|---|---|
| JDK 21 (Temurin) | `java -version` |
| Maven 3.9+ | `mvn -v` |
| Node.js LTS | `node -v` |
| [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools), unzipped to e.g. `C:\Android\platform-tools` | `adb version` |

Set the environment variables (in **System Properties → Environment Variables**):

```
ANDROID_HOME = C:\Android            (the folder that CONTAINS platform-tools)
Path        += C:\Android\platform-tools
```

Install Appium and its Android driver:

```bash
npm install -g appium
appium driver install uiautomator2
appium driver doctor uiautomator2      # should report no required fixes
```

### 3. Connect the tablet

**USB:** plug in the cable. When the tablet asks **"Allow USB debugging?"**, tap **Always allow** and then **Allow**.

```bash
adb devices
# List of devices attached
# HA1XXXXX    device        <- "device" means it's ready ("unauthorized" means you need to accept the prompt on the tablet)
```

**Wi-Fi (no cable, Android 11+):** this needs the laptop and the tablet on the same Wi-Fi.
1. On the tablet, go to **Developer options → Wireless debugging** and turn it on.
2. Tap **Pair device with pairing code**. Note the *IP:port* and the *code*, then run:

```bash
adb pair 192.168.1.25:41234          # enter the 6-digit code
adb connect 192.168.1.25:37891       # the IP:port shown on the Wireless debugging screen
adb devices
```

### 4. Start Appium (keep this window open)

```bash
appium --allow-insecure=uiautomator2:chromedriver_autodownload
```

The flag lets Appium download the chromedriver that matches the Chrome version on the tablet automatically.
On Appium 2, `--allow-insecure=chromedriver_autodownload` also works.

### 5. Run the tests

```bash
cd payments-test-automation

# TestNG UI tests on the tablet (suite testng-suites/tablet.xml)
mvn test -Dsuite=tablet

# Cucumber UI scenarios on the tablet
mvn test -Dsuite=cucumber -Dbrowser=android_chrome -Dcucumber.filter.tags="@ui"

# More than one device connected, or using Wi-Fi? Pin the tablet:
mvn test -Dsuite=tablet -Dappium.android.udid=HA1XXXXX
mvn test -Dsuite=tablet -Dappium.android.udid=192.168.1.25:37891
```

You will see Chrome open and the tests run on the tablet screen. The Extent report with screenshots taken from
the tablet goes to `target/reports/` on the laptop.

The `tablet` suite leaves out `mouseHover` and `multipleWindows`, because a touch screen has no mouse hover and no separate OS windows.

### Troubleshooting

| Problem | Fix |
|---|---|
| `adb devices` shows `unauthorized` | Unlock the tablet and accept the USB-debugging prompt. Or run `adb kill-server` and plug the cable in again |
| `adb devices` shows nothing | Try another cable or USB port (some cables are charge-only), and set USB mode to *File transfer* |
| `ANDROID_HOME is not set` | Set `ANDROID_HOME` as shown in step 2, then open a **new** terminal |
| `No Chromedriver found that can automate Chrome 1xx` | Start Appium with the `--allow-insecure` flag in step 4, or download the matching chromedriver and set `-Dappium.android.chromedriverExecutable=C:\drivers\chromedriver.exe` |
| `Connection refused 127.0.0.1:4723` | The Appium server isn't running (step 4) |
| Tests stop when the screen locks | Turn on *Stay awake* in Developer options and keep the tablet on charge |

---

## Option B: Run the framework on the tablet itself (Termux)

This is good for daily practice on the go: you can edit the code, run the SWIFT, DB and API tests, and read the report on the tablet.
Browser (Selenium) tests are **not** possible this way. Use Option A for those.

1. Install **Termux from F-Droid** (https://f-droid.org/packages/com.termux/). The Play Store version is outdated.
2. In Termux:

```bash
pkg update && pkg upgrade
pkg install openjdk-21 maven git        # if openjdk-21 isn't listed, run: pkg search openjdk
termux-setup-storage                     # allow access to Downloads (for opening reports)

git clone -b ccr-ab8fe2e5-ft2wte https://github.com/guravpraveen/selenium-automation.git
cd selenium-automation/payments-test-automation

mvn test -Dsuite=offline                                   # SWIFT MT/MX + database (H2), no internet needed
mvn test -Dsuite=api                                       # RestAssured (needs internet)
mvn test -Dsuite=cucumber -Dcucumber.filter.tags="not @ui" # Gherkin: API, DB, SWIFT
```

The first run downloads the Maven dependencies (about 200 MB), so use Wi-Fi.
If the repository is private, `git clone` asks for your GitHub username and a **personal access token** as the password.

3. To open the report in the tablet's Chrome:

```bash
cp -r target/reports ~/storage/downloads/payments-reports
```

Then open **Files → Downloads → payments-reports → ExtentReport_….html** in Chrome.

For editing code on the tablet, the **Acode** app (Play Store) can open the
`~/selenium-automation` folder. Or use `nano` / `vim` inside Termux.
