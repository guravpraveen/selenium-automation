package com.payments.automation.swift;

import java.util.ArrayList;
import java.util.List;

/** Collects all rule violations instead of failing at the first - better for test reporting. */
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();

    public void error(String code, String message) {
        errors.add(code + ": " + message);
    }

    public void check(boolean condition, String code, String message) {
        if (!condition) {
            error(code, message);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public boolean hasError(String code) {
        return errors.stream().anyMatch(e -> e.startsWith(code + ":"));
    }

    @Override
    public String toString() {
        return isValid() ? "VALID" : "INVALID " + errors;
    }
}
