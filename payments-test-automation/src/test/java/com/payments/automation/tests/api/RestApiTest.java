package com.payments.automation.tests.api;

import com.payments.automation.api.ApiClient;
import com.payments.automation.api.ResponseValidator;
import com.payments.automation.data.DataProviders;
import com.payments.automation.utils.FileUtils;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every RestAssured verb against https://jsonplaceholder.typicode.com (public fake REST API).
 * Replace with your payment-hub endpoints: /payments, /payments/{uetr}/status, /liquidity/positions ...
 */
public class RestApiTest {

    /** ApiClient holds per-request state, so each test (thread) gets its own instance - the suite runs in parallel. */
    private ApiClient api() {
        return new ApiClient();
    }

    @Test(groups = {"api", "smoke"}, description = "GET single resource + JSON schema validation")
    public void getSinglePost() {
        Response r = api().pathParam("id", 1).get("/posts/{id}");
        ResponseValidator.of(r)
                .statusCode(200)
                .contentType("application/json")
                .responseTimeBelow(10000)
                .jsonValue("id", 1)
                .jsonValue("userId", 1)
                .jsonNotNull("title")
                .matchesSchema("schemas/post-schema.json");
    }

    @Test(groups = "api", description = "GET with query parameters returns filtered list")
    public void getWithQueryParams() {
        Response r = api().queryParam("userId", 1).get("/posts");
        ResponseValidator v = ResponseValidator.of(r).statusCode(200).jsonArraySize("$", 10);
        List<Integer> userIds = v.json().getList("userId");
        Assertions.assertThat(userIds).containsOnly(1);
    }

    @Test(groups = "api", description = "GET nested object + schema")
    public void getUserNestedFields() {
        Response r = api().get("/users/1");
        ResponseValidator.of(r)
                .statusCode(200)
                .jsonValue("address.city", "Gwenborough")
                .matchesSchema("schemas/user-schema.json");
    }

    @Test(groups = "api", description = "POST with Map body")
    public void createPostWithMap() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 7);
        body.put("title", "EOD liquidity sweep");
        body.put("body", "USD nostro sweep to treasury");
        Response r = api().header("X-Correlation-Id", "corr-001").post("/posts", body);
        ResponseValidator.of(r).statusCode(201).jsonValue("title", "EOD liquidity sweep").jsonNotNull("id");
    }

    @Test(groups = "api", description = "POST with JSON file body")
    public void createPostFromJsonFile() {
        String json = FileUtils.readResource("testdata/json/create-post.json");
        Response r = api().body(json).post("/posts");
        ResponseValidator.of(r).statusCode(201).jsonValue("title", "Liquidity sweep EOD");
    }

    @Test(groups = "api", description = "POST with POJO body, deserialise response to POJO")
    public void createPostWithPojo() {
        Post request = new Post(3, null, "Payment repair", "Repair queue item 42");
        Post created = ResponseValidator.of(api().post("/posts", request)).statusCode(201).as(Post.class);
        Assertions.assertThat(created.title()).isEqualTo("Payment repair");
        Assertions.assertThat(created.id()).isPositive();
    }

    @Test(groups = "api", dataProvider = "apiPosts", dataProviderClass = DataProviders.class,
            description = "Data-driven POST from Excel")
    public void createPostFromExcel(Map<String, String> row) {
        Post request = new Post(Integer.parseInt(row.get("UserId")), null, row.get("Title"), row.get("Body"));
        ResponseValidator.of(api().post("/posts", request))
                .statusCode(Integer.parseInt(row.get("ExpectedStatus")))
                .jsonValue("title", row.get("Title"));
    }

    @Test(groups = "api", description = "PUT replaces the full resource")
    public void updatePostWithPut() {
        Post request = new Post(1, 1, "Updated title", "Updated body");
        ResponseValidator.of(api().pathParam("id", 1).put("/posts/{id}", request))
                .statusCode(200)
                .jsonValue("title", "Updated title")
                .jsonValue("body", "Updated body");
    }

    @Test(groups = "api", description = "PATCH updates one field")
    public void partialUpdateWithPatch() {
        ResponseValidator.of(api().patch("/posts/1", Map.of("title", "Patched title")))
                .statusCode(200)
                .jsonValue("title", "Patched title")
                .jsonValue("id", 1);
    }

    @Test(groups = "api", description = "DELETE resource")
    public void deletePost() {
        ResponseValidator.of(api().delete("/posts/1")).statusCode(200);
    }

    @Test(groups = "api", description = "HEAD returns headers but no body")
    public void headRequest() {
        Response r = api().head("/posts/1");
        ResponseValidator.of(r).statusCode(200).headerPresent("Content-Type");
        Assertions.assertThat(r.asString()).isEmpty();
    }

    @Test(groups = "api", description = "OPTIONS lists allowed methods (CORS preflight)")
    public void optionsRequest() {
        Response r = api().header("Origin", "https://example.com")
                .header("Access-Control-Request-Method", "POST")
                .options("/posts");
        ResponseValidator.of(r).statusIn(200, 204);
        Assertions.assertThat(r.getHeader("Access-Control-Allow-Methods")).contains("POST");
    }

    @Test(groups = "api", description = "Negative: unknown resource gives 404")
    public void notFound() {
        ResponseValidator.of(api().get("/posts/999999")).statusCode(404);
    }

    @Test(groups = "api", description = "Form-urlencoded request body")
    public void formParams() {
        Response r = api().formParam("title", "form-title").formParam("userId", "5")
                .accept(ContentType.JSON).post("/posts");
        ResponseValidator.of(r).statusCode(201).jsonValue("title", "form-title");
    }

    /** Java record POJO - Jackson serialises/deserialises it automatically. */
    public record Post(Integer userId, Integer id, String title, String body) {
    }
}
