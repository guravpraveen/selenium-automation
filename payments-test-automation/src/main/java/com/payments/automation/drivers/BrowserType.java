package com.payments.automation.drivers;

/** Every target the {@link DriverFactory} knows how to start. */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE,
    SAFARI,
    /** Desktop Chrome emulating a phone (no device needed - great for practice). */
    CHROME_MOBILE_EMULATION,
    /** Real device / emulator Chrome through an Appium server. */
    ANDROID_CHROME,
    /** Real device / simulator Safari through an Appium server. */
    IOS_SAFARI;

    public static BrowserType from(String value) {
        return valueOf(value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
    }

    public boolean isAppium() {
        return this == ANDROID_CHROME || this == IOS_SAFARI;
    }
}
