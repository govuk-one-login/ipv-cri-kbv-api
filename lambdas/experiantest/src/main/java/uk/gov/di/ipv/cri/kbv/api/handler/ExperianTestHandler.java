package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.assertions.AssertionTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.KeyStoreTest;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.ImportCertificateTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.SOAPRequestTest;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.SSLHandshakeTest;
import uk.gov.di.ipv.cri.kbv.api.utils.SecretsGrabber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExperianTestHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ExperianTestHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<Test<?>> testSuits;

    public ExperianTestHandler() {
        this(buildSecretsManagerClient());
    }

    public ExperianTestHandler(SecretsManagerClient secretsManagerClient) {
        Objects.requireNonNull(secretsManagerClient, "SecretsManagerClient cannot be null");

        try {
            SecretsGrabber secretsGrabber = new SecretsGrabber(secretsManagerClient);
            String waspUrl = secretsGrabber.getSecretValue(Configuration.WASP_URL_SECRET);
            String keystorePassword = secretsGrabber.getSecretValue(Configuration.KEYSTORE_PASSWORD);

            LOGGER.info("WASP URL: {}", waspUrl);

            this.testSuits =
                    List.of(
                            new SSLHandshakeTest(),
                            new SOAPRequestTest(keystorePassword, waspUrl),
                            new ImportCertificateTest(keystorePassword),
                            new KeyStoreTest(keystorePassword));

            initializeKeystore(secretsGrabber);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize ExperianTestHandler", e);
        }
    }

    @Override
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

            return createResponse(loginWithCertificateReport.isSoapTokenValid() ? 200 : 416, "");

        } catch (Exception e) {
            LOGGER.error("Test execution failed", e);
            return createErrorResponse(e);
        }
    }

    private void initializeKeystore(SecretsGrabber secretsGrabber) throws IOException {
        LOGGER.info("Initializing keystore at: {}", Configuration.JKS_FILE_LOCATION);
        createKeyStoreFile(
                Configuration.JKS_FILE_LOCATION, secretsGrabber.getSecretValue(Configuration.KEYSTORE_SECRET));
    }

    public static void createKeyStoreFile(String keyStoreLocation, String base64KeyStore)
            throws IOException {
        try {
            Path path = Paths.get(keyStoreLocation);
            byte[] decodedBytes = Base64.getDecoder().decode(base64KeyStore);
            Files.write(path, decodedBytes);
            LOGGER.debug("Keystore file created successfully at: {}", keyStoreLocation);
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to decode Base64 keystore content", e);
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

        AssertionTest assertionTest = new AssertionTest(reports.values().toArray(new Object[0]));
        reports.put(assertionTest.getClass().getSimpleName(), assertionTest.run());

        return reports;
    }

    private static SecretsManagerClient buildSecretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(body);
    }

    private static APIGatewayProxyResponseEvent createErrorResponse(Exception e) {
        String errorBody = String.format("{\"error\": \"%s\"}", e.getMessage());
        return createResponse(500, errorBody);
    }
}
