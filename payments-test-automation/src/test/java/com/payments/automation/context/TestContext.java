package com.payments.automation.context;

import com.payments.automation.api.ApiClient;
import com.payments.automation.db.DatabaseClient;
import com.payments.automation.swift.MtMessage;
import com.payments.automation.swift.MxMessage;
import com.payments.automation.swift.PaymentInstruction;
import com.payments.automation.swift.ValidationResult;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario-scoped state shared between step definition classes.
 * PicoContainer creates one instance per scenario and injects it into every step class constructor.
 */
public class TestContext {

    private ApiClient apiClient;
    public Response response;
    public DatabaseClient db;
    public List<Map<String, Object>> dbRows;
    public PaymentInstruction payment;
    public MtMessage mtMessage;
    public MxMessage mxMessage;
    public String rawMessage;
    public ValidationResult validationResult;
    private final Map<String, Object> data = new HashMap<>();

    public ApiClient api() {
        if (apiClient == null) {
            apiClient = new ApiClient();
        }
        return apiClient;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
}
