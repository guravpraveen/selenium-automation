package com.payments.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration in this order (later wins):
 * <ol>
 *   <li>config/config.properties   - common defaults</li>
 *   <li>config/{env}.properties     - environment specific (qa, uat, sit ...)</li>
 *   <li>config/db.properties        - database connection details</li>
 *   <li>Environment variables       - e.g. ORACLE_PASSWORD for ${ORACLE_PASSWORD}</li>
 *   <li>JVM system properties       - mvn test -Dbrowser=firefox</li>
 * </ol>
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        load("config/config.properties", true);
        String env = System.getProperty("env", PROPS.getProperty("env", "qa"));
        PROPS.setProperty("env", env);
        load("config/" + env + ".properties", false);
        load("config/db.properties", false);
    }

    private ConfigReader() {
    }

    private static void load(String resource, boolean mandatory) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (mandatory) {
                    throw new IllegalStateException("Missing configuration file on classpath: " + resource);
                }
                return;
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + resource, e);
        }
    }

    /** Returns the value, resolving system-property overrides and ${ENV_VAR} placeholders. */
    public static String get(String key) {
        String value = System.getProperty(key, PROPS.getProperty(key));
        return value == null ? null : resolvePlaceholders(value.trim());
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        return value == null || value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null || value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Required configuration key not set: " + key);
        }
        return value;
    }

    private static String resolvePlaceholders(String value) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int start = value.indexOf("${", i);
            if (start < 0) {
                out.append(value.substring(i));
                break;
            }
            int end = value.indexOf('}', start);
            if (end < 0) {
                out.append(value.substring(i));
                break;
            }
            out.append(value, i, start);
            String name = value.substring(start + 2, end);
            String replacement = System.getenv(name);
            if (replacement == null) {
                replacement = System.getProperty(name, "");
            }
            out.append(replacement);
            i = end + 1;
        }
        return out.toString();
    }
}
