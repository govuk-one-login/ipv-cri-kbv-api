package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.assertions.AssertionTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.KeyStoreTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.ImportCertificateTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.SOAPRequestTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.SSLHandshakeTest;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.report.SSLHandshakeTestReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ThirdPartyHealthCheckHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ThirdPartyHealthCheckHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int SUCCESS_HTTP_CODE = 200;
    private static final int FAIL_HTTP_CODE = 525;

    private final List<Test<?>> testSuits;

    @ExcludeFromGeneratedCoverageReport
    public ThirdPartyHealthCheckHandler() {
        this(new ServiceFactory());
    }

    public ThirdPartyHealthCheckHandler(ServiceFactory serviceFactory) {
        try {
            String waspUrl = serviceFactory.getSecretsProvider().get(Configuration.WASP_URL_SECRET);
            String keystorePassword =
                    serviceFactory.getSecretsProvider().get(Configuration.KEYSTORE_PASSWORD);
            String keystoreSecret =
                    serviceFactory.getSecretsProvider().get(Configuration.KEYSTORE_SECRET);

            LOGGER.info("WASP URL: {}", waspUrl);

            this.testSuits =
                    List.of(
                            new ImportCertificateTest(keystorePassword),
                            new SSLHandshakeTest(),
                            new SOAPRequestTest(keystorePassword, waspUrl),
                            new KeyStoreTest(keystorePassword));

            createKeyStoreFile(keystoreSecret);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize ExperianTestHandler", e);
        }
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, Object> testReports = performTests();
            String reportJson = OBJECT_MAPPER.writeValueAsString(testReports);

            LOGGER.info("Generated test report:\n{}", reportJson);

            if (request.getPath().endsWith("/info")) {
                return createResponse(200, reportJson);
            }

            LoginWithCertificateTestReport loginWithCertificateReport =
                    (LoginWithCertificateTestReport)
                            testReports.get(SOAPRequestTest.class.getSimpleName());

            SSLHandshakeTestReport sslHandshakeTestReport =
                    (SSLHandshakeTestReport)
                            testReports.get(SSLHandshakeTest.class.getSimpleName());

            return createResponse(
                    loginWithCertificateReport.isSoapTokenValid()
                                    && sslHandshakeTestReport.isSessionValid()
                            ? SUCCESS_HTTP_CODE
                            : FAIL_HTTP_CODE,
                    "");

        } catch (Exception e) {
            LOGGER.error("Test execution failed", e);
            return createErrorResponse(e);
        }
    }

    private Map<String, Object> performTests() {
        Map<String, Object> reports = new HashMap<>();

        for (Test<?> test : testSuits) {
            LOGGER.info("Running test {}", test.getClass().getName());
            try {
                reports.put(test.getClass().getSimpleName(), test.run());
            } catch (Exception e) {
                LOGGER.error("{} threw an exception", test.getClass().getName(), e);
            }
        }

        AssertionTest assertionTest = generateAssertionTestReport(reports);
        reports.put(assertionTest.getClass().getSimpleName(), assertionTest.run());

        return reports;
    }

    private static void createKeyStoreFile(String base64KeyStore) throws IOException {
        LOGGER.info("Initializing keystore at: {}", Configuration.JKS_FILE_LOCATION);

        try {
            Path path = Paths.get(Configuration.JKS_FILE_LOCATION).normalize();
            byte[] decodedBytes = Base64.getDecoder().decode(base64KeyStore);
            Files.write(path, decodedBytes);
            LOGGER.info(
                    "Keystore file created successfully at: {}", Configuration.JKS_FILE_LOCATION);
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to decode Base64 keystore content", e);
        }
    }

    private static AssertionTest generateAssertionTestReport(Map<String, Object> reports) {
        return new AssertionTest(reports.values().toArray(new Object[0]));
    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(body);
    }

    private static APIGatewayProxyResponseEvent createErrorResponse(Exception e) {
        String errorBody = String.format("{\"error\": \"%s\"}", e.getMessage());
        return createResponse(500, errorBody);
    }
}
