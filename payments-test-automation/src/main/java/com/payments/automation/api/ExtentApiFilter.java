package com.payments.automation.api;

import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.payments.automation.reports.ExtentTestManager;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Writes every request / response into the Extent report and the log file (Authorization masked). */
public class ExtentApiFilter implements Filter {

    private static final Logger LOG = LogManager.getLogger(ExtentApiFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification res, FilterContext ctx) {
        long start = System.currentTimeMillis();
        Response response = ctx.next(req, res);
        long took = System.currentTimeMillis() - start;

        String requestBody = req.getBody() == null ? "" : req.getBody().toString();
        String headers = req.getHeaders().asList().stream()
                .map(h -> h.getName() + ": " + (h.getName().equalsIgnoreCase("Authorization") ? "****" : h.getValue()))
                .reduce((a, b) -> a + "\n" + b).orElse("");
        String responseBody = response.getBody().asPrettyString();

        LOG.info("{} {} -> {} ({} ms)", req.getMethod(), req.getURI(), response.getStatusCode(), took);
        LOG.debug("Request body: {}\nResponse body: {}", requestBody, responseBody);

        if (ExtentTestManager.getTest() != null) {
            ExtentTestManager.getTest().info(MarkupHelper.createLabel(
                    req.getMethod() + " " + req.getURI() + " -> " + response.getStatusCode() + " (" + took + " ms)",
                    response.getStatusCode() < 400
                            ? com.aventstack.extentreports.markuputils.ExtentColor.GREEN
                            : com.aventstack.extentreports.markuputils.ExtentColor.RED));
            ExtentTestManager.code("Request headers", headers);
            if (!requestBody.isEmpty()) {
                ExtentTestManager.code("Request body", requestBody);
            }
            ExtentTestManager.code("Response body", responseBody.length() > 5000
                    ? responseBody.substring(0, 5000) + "\n... (truncated)" : responseBody);
        }
        return response;
    }
}
