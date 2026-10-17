package com.payments.automation.api;

import com.payments.automation.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.time.Instant;

/**
 * OAuth2 client-credentials token, fetched once and cached until 60s before expiry.
 * Typical for SWIFT gpi / API Gateway / internal payment-hub APIs.
 *
 * <pre>
 * oauth.token.url=https://auth.example.com/oauth2/token
 * oauth.client.id=${OAUTH_CLIENT_ID}
 * oauth.client.secret=${OAUTH_CLIENT_SECRET}
 * oauth.scope=payments.read payments.write
 * </pre>
 */
public final class TokenManager {

    private static String token;
    private static Instant expiry = Instant.EPOCH;

    private TokenManager() {
    }

    public static synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiry)) {
            Response r = RestAssured.given()
                    .auth().preemptive().basic(ConfigReader.getRequired("oauth.client.id"),
                            ConfigReader.getRequired("oauth.client.secret"))
                    .formParam("grant_type", "client_credentials")
                    .formParam("scope", ConfigReader.get("oauth.scope", ""))
                    .post(ConfigReader.getRequired("oauth.token.url"));
            if (r.getStatusCode() != 200) {
                throw new IllegalStateException("Token request failed: " + r.getStatusCode() + " " + r.asString());
            }
            token = r.jsonPath().getString("access_token");
            int expiresIn = r.jsonPath().getInt("expires_in");
            expiry = Instant.now().plusSeconds(Math.max(expiresIn - 60, 0));
        }
        return token;
    }

    public static synchronized void invalidate() {
        token = null;
    }
}
