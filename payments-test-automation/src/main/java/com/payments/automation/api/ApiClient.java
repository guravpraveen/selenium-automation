package com.payments.automation.api;

import com.payments.automation.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.util.Map;

/**
 * Fluent wrapper over RestAssured covering every HTTP verb and the common request options.
 *
 * <pre>
 * Response r = new ApiClient()
 *         .bearerToken(token)
 *         .header("X-Correlation-Id", uetr)
 *         .body(paymentRequest)
 *         .post("/payments");
 * </pre>
 *
 * Each verb call sends the request and then resets the per-request state, so one client
 * can be reused for a sequence of calls (base URI and default headers are kept).
 * Not thread-safe: use one instance per test / thread when running in parallel.
 */
public class ApiClient {

    private final String baseUri;
    private RequestSpecification request;

    public ApiClient() {
        this(ConfigReader.getRequired("api.base.url"));
    }

    public ApiClient(String baseUri) {
        this.baseUri = baseUri;
        reset();
    }

    /** Re-creates the request with only the defaults (base URI, JSON, logging filter). */
    public ApiClient reset() {
        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", ConfigReader.getInt("api.timeout.ms", 30000))
                        .setParam("http.socket.timeout", ConfigReader.getInt("api.timeout.ms", 30000)))
                // RestAssured defaults to ISO-8859-1 for form / text bodies; most modern APIs expect UTF-8
                .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));
        if (ConfigReader.getBoolean("api.relaxed.https", false)) {
            config = config.sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
        }
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setConfig(config)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new ExtentApiFilter());
        if (ConfigReader.getBoolean("api.log.console", false)) {
            builder.log(LogDetail.ALL);
        }
        request = RestAssured.given().spec(builder.build());
        return this;
    }

    /** Direct access for anything not wrapped here. */
    public RequestSpecification spec() {
        return request;
    }

    // ------------------------------------------------------------------ request building
    public ApiClient header(String name, Object value) {
        request.header(name, value);
        return this;
    }

    public ApiClient headers(Map<String, ?> headers) {
        request.headers(headers);
        return this;
    }

    public ApiClient queryParam(String name, Object... values) {
        request.queryParam(name, values);
        return this;
    }

    public ApiClient queryParams(Map<String, ?> params) {
        request.queryParams(params);
        return this;
    }

    public ApiClient pathParam(String name, Object value) {
        request.pathParam(name, value);
        return this;
    }

    public ApiClient pathParams(Map<String, ?> params) {
        request.pathParams(params);
        return this;
    }

    public ApiClient formParam(String name, Object... values) {
        request.contentType(ContentType.URLENC).formParam(name, values);
        return this;
    }

    public ApiClient formParams(Map<String, ?> params) {
        request.contentType(ContentType.URLENC).formParams(params);
        return this;
    }

    public ApiClient cookie(String name, Object value) {
        request.cookie(name, value);
        return this;
    }

    public ApiClient cookies(Cookies cookies) {
        request.cookies(cookies);
        return this;
    }

    /** POJO / Map (serialised by Jackson) or raw String / byte[] body. */
    public ApiClient body(Object body) {
        request.body(body);
        return this;
    }

    public ApiClient bodyFromFile(File file) {
        request.body(file);
        return this;
    }

    public ApiClient contentType(ContentType type) {
        request.contentType(type);
        return this;
    }

    public ApiClient contentType(String type) {
        request.contentType(type);
        return this;
    }

    public ApiClient accept(ContentType type) {
        request.accept(type);
        return this;
    }

    /** ISO 20022 / SWIFT MX payloads are XML. */
    public ApiClient xmlBody(String xml) {
        request.contentType(ContentType.XML).accept(ContentType.XML).body(xml);
        return this;
    }

    public ApiClient multipart(String controlName, File file) {
        request.contentType(ContentType.MULTIPART).multiPart(controlName, file);
        return this;
    }

    public ApiClient multipart(String controlName, String fileName, byte[] content, String mimeType) {
        request.contentType(ContentType.MULTIPART).multiPart(controlName, fileName, content, mimeType);
        return this;
    }

    // ------------------------------------------------------------------ authentication
    public ApiClient basicAuth(String user, String password) {
        request.auth().preemptive().basic(user, password);
        return this;
    }

    public ApiClient digestAuth(String user, String password) {
        request.auth().digest(user, password);
        return this;
    }

    public ApiClient bearerToken(String token) {
        request.header("Authorization", "Bearer " + token);
        return this;
    }

    public ApiClient oauth2(String accessToken) {
        request.auth().oauth2(accessToken);
        return this;
    }

    public ApiClient apiKey(String headerName, String key) {
        request.header(headerName, key);
        return this;
    }

    public ApiClient noAuth() {
        request.auth().none();
        return this;
    }

    // ------------------------------------------------------------------ HTTP verbs
    public Response get(String path) {
        return send(Method.GET, path);
    }

    public Response post(String path) {
        return send(Method.POST, path);
    }

    public Response put(String path) {
        return send(Method.PUT, path);
    }

    public Response patch(String path) {
        return send(Method.PATCH, path);
    }

    public Response delete(String path) {
        return send(Method.DELETE, path);
    }

    public Response head(String path) {
        return send(Method.HEAD, path);
    }

    public Response options(String path) {
        return send(Method.OPTIONS, path);
    }

    /** Convenience overloads that set the body and send in one call. */
    public Response post(String path, Object body) {
        return body(body).post(path);
    }

    public Response put(String path, Object body) {
        return body(body).put(path);
    }

    public Response patch(String path, Object body) {
        return body(body).patch(path);
    }

    public Response send(Method method, String path) {
        try {
            return request.when().request(method, path).then().extract().response();
        } finally {
            reset();
        }
    }

    /** Downloads a binary response (PDF statement, CSV report...). */
    public byte[] download(String path) {
        try {
            return request.accept(ContentType.ANY).when().get(path).then().extract().asByteArray();
        } finally {
            reset();
        }
    }
}
