package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.tests.ErrorReport;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.assertions.AssertionTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.KeyStoreTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.ImportCertificateTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.SOAPRequestTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.SSLHandshakeTest;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.report.SSLHandshakeTestReport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThirdPartyHealthCheckHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ThirdPartyHealthCheckHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int SUCCESS_HTTP_CODE = 200;
    private static final int FAIL_HTTP_CODE = 525;

    private final List<Test<?>> testSuits;

    @ExcludeFromGeneratedCoverageReport
    public ThirdPartyHealthCheckHandler() throws IOException {
        this(new ServiceFactory());
    }

    public ThirdPartyHealthCheckHandler(ServiceFactory serviceFactory) throws IOException {
        ConfigurationService configurationService =
                new ConfigurationService(
                        serviceFactory.getSsmProvider(), serviceFactory.getSecretsProvider());

        String waspUrl = configurationService.getSecretValue(Configuration.WASP_URL_SECRET);
        String keystorePassword =
                configurationService.getSecretValue(Configuration.KEYSTORE_PASSWORD);
        String keystoreSecret = configurationService.getSecretValue(Configuration.KEYSTORE_SECRET);

        LOGGER.info("WASP URL: {}", waspUrl);
        LOGGER.info("keystorePassword: {}", keystorePassword);
        LOGGER.info("keystoreSecret: {}", keystoreSecret);

        this.testSuits =
                List.of(
                        new ImportCertificateTest(keystoreSecret, keystorePassword),
                        new SSLHandshakeTest(Configuration.WASP_HOST, Configuration.WASP_PORT),
                        new SOAPRequestTest(keystoreSecret, keystorePassword, waspUrl),
                        new KeyStoreTest(keystoreSecret, keystorePassword));
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return handleRequestWithoutBody(request);
            } else {
                return handleRequestWithBody(request);
            }
        } catch (Exception e) {
            LOGGER.error("ThirdPartyHealthCheckHandler threw an exception", e);
            return createErrorResponse(500, e);
        }
    }

    private APIGatewayProxyResponseEvent handleRequestWithBody(APIGatewayProxyRequestEvent request)
            throws IOException {
        LOGGER.info("Received request with body");

        RequestPayload payload;

        try {
            payload = OBJECT_MAPPER.readValue(request.getBody(), RequestPayload.class);
        } catch (JsonProcessingException e) {
            return createErrorResponse(400, new IllegalArgumentException("Invalid request body"));
        }

        List<Test<?>> tests = getTestsFromRequestPayload(payload);
        Map<String, Object> runs = performTests(tests);
        String reports = generateTestReport(runs);

        return createResponse(200, reports);
    }

    private APIGatewayProxyResponseEvent handleRequestWithoutBody(
            APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        LOGGER.info("Received request without body");

        Map<String, Object> testReports = performTests(this.testSuits);

        AssertionTest assertionTest = generateAssertionTestReport(testReports);
        testReports.put(assertionTest.getClass().getSimpleName(), assertionTest.run());

        String reportJson = generateTestReport(testReports);

        if (request != null && request.getPath() != null && request.getPath().endsWith("/info")) {
            return createResponse(200, reportJson);
        }

        LoginWithCertificateTestReport loginWithCertificateReport =
                (LoginWithCertificateTestReport)
                        testReports.get(SOAPRequestTest.class.getSimpleName());

        SSLHandshakeTestReport sslHandshakeTestReport =
                (SSLHandshakeTestReport) testReports.get(SSLHandshakeTest.class.getSimpleName());

        return createResponse(
                loginWithCertificateReport.isSoapTokenValid()
                                && sslHandshakeTestReport.isSessionValid()
                        ? SUCCESS_HTTP_CODE
                        : FAIL_HTTP_CODE,
                "");
    }

    private static List<Test<?>> getTestsFromRequestPayload(RequestPayload payload)
            throws IOException {
        List<Test<?>> tests = new ArrayList<>();
        for (String requestedTest : payload.getTests()) {
            if (requestedTest.equalsIgnoreCase(AssertionTest.class.getSimpleName())) {
                tests.add(new AssertionTest());
            } else if (requestedTest.equalsIgnoreCase(KeyStoreTest.class.getSimpleName())) {
                tests.add(
                        new KeyStoreTest(
                                payload.getBase64Keystore(), payload.getKeystorePassword()));
            } else if (requestedTest.equalsIgnoreCase(
                    ImportCertificateTest.class.getSimpleName())) {
                tests.add(
                        new ImportCertificateTest(
                                payload.getBase64Keystore(), payload.getBase64Keystore()));
            } else if (requestedTest.equalsIgnoreCase(SOAPRequestTest.class.getSimpleName())) {
                tests.add(
                        new SOAPRequestTest(
                                payload.getBase64Keystore(),
                                payload.getKeystorePassword(),
                                payload.getHost()));
            } else if (requestedTest.equalsIgnoreCase(SSLHandshakeTest.class.getSimpleName())) {
                tests.add(new SSLHandshakeTest(payload.getHost(), payload.getPort()));
            }
        }
        return tests;
    }

    private static Map<String, Object> performTests(List<Test<?>> testSuits) {
        LOGGER.info("Performing {} tests", testSuits.size());
        Map<String, Object> reports = new HashMap<>();

        for (int i = 0; i < testSuits.size(); i++) {
            Test<?> test = testSuits.get(i);
            LOGGER.info("({}) Running test {}", i + 1, test.getClass().getName());
            try {
                reports.put(test.getClass().getSimpleName(), test.run());
            } catch (Exception e) {
                reports.put(test.getClass().getSimpleName(), new ErrorReport(e.getMessage(), true));
                LOGGER.error("{} threw an exception", test.getClass().getName(), e);
            }
        }

        LOGGER.info("All tests have finished running");

        return reports;
    }

    private static String generateTestReport(Map<String, Object> testReports)
            throws JsonProcessingException {
        LOGGER.info("Generating test report");
        String reportJson = OBJECT_MAPPER.writeValueAsString(testReports);
        LOGGER.info(reportJson);
        return reportJson;
    }

    private static AssertionTest generateAssertionTestReport(Map<String, Object> reports) {
        return new AssertionTest(reports.values().toArray(new Object[0]));
    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(body);
    }

    private static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, Exception e) {
        String errorBody = String.format("{\"error\": \"%s\"}", e.getMessage());
        return createResponse(statusCode, errorBody);
    }
}
