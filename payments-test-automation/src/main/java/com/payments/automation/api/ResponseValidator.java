package com.payments.automation.api;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * Fluent assertions on a RestAssured {@link Response}.
 *
 * <pre>
 * ResponseValidator.of(response)
 *         .statusCode(201)
 *         .responseTimeBelow(2000)
 *         .header("Content-Type", "application/json; charset=utf-8")
 *         .jsonValue("status", "ACCEPTED")
 *         .matchesSchema("schemas/post-schema.json");
 * </pre>
 */
public final class ResponseValidator {

    private final Response response;

    private ResponseValidator(Response response) {
        this.response = response;
    }

    public static ResponseValidator of(Response response) {
        return new ResponseValidator(response);
    }

    public ResponseValidator statusCode(int expected) {
        Assertions.assertThat(response.getStatusCode())
                .as("HTTP status. Body: %s", response.asString())
                .isEqualTo(expected);
        return this;
    }

    public ResponseValidator statusIn(Integer... expected) {
        Assertions.assertThat(response.getStatusCode()).isIn((Object[]) expected);
        return this;
    }

    public ResponseValidator responseTimeBelow(long millis) {
        Assertions.assertThat(response.getTime()).as("response time ms").isLessThan(millis);
        return this;
    }

    public ResponseValidator header(String name, String expected) {
        Assertions.assertThat(response.getHeader(name)).as("header " + name).isEqualTo(expected);
        return this;
    }

    public ResponseValidator headerContains(String name, String fragment) {
        Assertions.assertThat(response.getHeader(name)).as("header " + name).contains(fragment);
        return this;
    }

    public ResponseValidator headerPresent(String name) {
        Assertions.assertThat(response.getHeaders().hasHeaderWithName(name)).as("header " + name + " present").isTrue();
        return this;
    }

    public ResponseValidator contentType(String fragment) {
        Assertions.assertThat(response.getContentType()).contains(fragment);
        return this;
    }

    public ResponseValidator bodyContains(String text) {
        Assertions.assertThat(response.asString()).contains(text);
        return this;
    }

    /** JSONPath (Groovy GPath) value check: "data.id", "items[0].amount", "items.size()". */
    public ResponseValidator jsonValue(String path, Object expected) {
        Object actual = response.jsonPath().get(path);
        Assertions.assertThat(String.valueOf(actual)).as("json path " + path).isEqualTo(String.valueOf(expected));
        return this;
    }

    public ResponseValidator jsonNotNull(String path) {
        Assertions.assertThat((Object) response.jsonPath().get(path)).as("json path " + path).isNotNull();
        return this;
    }

    public ResponseValidator jsonArraySize(String path, int expected) {
        List<Object> list = response.jsonPath().getList(path);
        Assertions.assertThat(list).as("json array " + path).hasSize(expected);
        return this;
    }

    public ResponseValidator xmlValue(String path, String expected) {
        Assertions.assertThat(response.xmlPath().getString(path)).as("xml path " + path).isEqualTo(expected);
        return this;
    }

    /** Validates the body against a JSON schema on the classpath (src/test/resources/schemas). */
    public ResponseValidator matchesSchema(String classpathSchema) {
        response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema));
        return this;
    }

    public JsonPath json() {
        return response.jsonPath();
    }

    public XmlPath xml() {
        return response.xmlPath();
    }

    public <T> T as(Class<T> type) {
        return response.as(type);
    }

    public Response response() {
        return response;
    }
}
