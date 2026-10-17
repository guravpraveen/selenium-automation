package com.payments.automation.stepdefs;

import com.payments.automation.api.ResponseValidator;
import com.payments.automation.context.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;

/** Generic, reusable REST steps - write API scenarios without new Java code. */
public class ApiSteps {

    private final TestContext ctx;

    public ApiSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("the request header {string} is {string}")
    public void header(String name, String value) {
        ctx.api().header(name, value);
    }

    @Given("the query parameter {string} is {string}")
    public void queryParam(String name, String value) {
        ctx.api().queryParam(name, value);
    }

    @Given("the request body is:")
    public void body(DataTable table) {
        Map<String, Object> body = new HashMap<>();
        table.asMap(String.class, String.class).forEach((k, v) -> body.put(String.valueOf(k), autoType(String.valueOf(v))));
        ctx.api().body(body);
    }

    @Given("the request JSON is:")
    public void bodyJson(String docString) {
        ctx.api().body(docString);
    }

    @When("I send a {word} request to {string}")
    public void send(String method, String path) {
        ctx.response = switch (method.toUpperCase()) {
            case "GET" -> ctx.api().get(path);
            case "POST" -> ctx.api().post(path);
            case "PUT" -> ctx.api().put(path);
            case "PATCH" -> ctx.api().patch(path);
            case "DELETE" -> ctx.api().delete(path);
            case "HEAD" -> ctx.api().head(path);
            case "OPTIONS" -> ctx.api().options(path);
            default -> throw new IllegalArgumentException("Unsupported method " + method);
        };
    }

    @Then("the response status code should be {int}")
    public void status(int code) {
        ResponseValidator.of(ctx.response).statusCode(code);
    }

    @Then("the response field {string} should be {string}")
    public void field(String path, String expected) {
        ResponseValidator.of(ctx.response).jsonValue(path, expected);
    }

    @Then("the response should contain fields:")
    public void fields(DataTable table) {
        ResponseValidator v = ResponseValidator.of(ctx.response);
        table.asMap(String.class, String.class).forEach((k, e) -> v.jsonValue(String.valueOf(k), e));
    }

    @Then("the response list {string} should have {int} items")
    public void listSize(String path, int size) {
        ResponseValidator.of(ctx.response).jsonArraySize(path, size);
    }

    @Then("the response should match schema {string}")
    public void schema(String schema) {
        ResponseValidator.of(ctx.response).matchesSchema(schema);
    }

    @Then("the response time should be below {long} ms")
    public void responseTime(long ms) {
        ResponseValidator.of(ctx.response).responseTimeBelow(ms);
    }

    private static Object autoType(String v) {
        return v.matches("-?\\d+") ? Integer.valueOf(v) : v;
    }
}
