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
import uk.gov.di.ipv.cri.kbv.api.models.*;
import uk.gov.di.ipv.cri.kbv.api.utils.*;

import javax.net.ssl.SSLPeerUnverifiedException;

import java.io.File;
import java.io.IOException;

public class ExperianTestHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger(ExperianTestHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final class Config {
        static final String JKS_FILE_LOCATION = "/tmp/experian.jks";
        static final String PFX_FILE_LOCATION = "/tmp/experian.pfx";
        static final String WASP_HOST = "secure.wasp.uk.experian.com";
        static final int WASP_PORT = 443;

        static final String WASP_URL_SECRET = System.getenv("WaspURLSecret");
        static final String KEYSTORE_SECRET = System.getenv("KeyStoreSecret");
        static final String KEYSTORE_PASSWORD = System.getenv("KeyStorePassword");
    }

    private final String waspUrl;
    private final String keystorePassword;

    public ExperianTestHandler() {
        this(secretsManagerClient());
    }

    // Constructor for testing
    ExperianTestHandler(SecretsManagerClient secretsManagerClient) {
        try {
            SecretsGrabber secretsGrabber = new SecretsGrabber(secretsManagerClient);
            this.waspUrl = secretsGrabber.getSecretValue(Config.WASP_URL_SECRET);
            this.keystorePassword = secretsGrabber.getSecretValue(Config.KEYSTORE_PASSWORD);
            initializeKeystore(secretsGrabber);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize handler", e);
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            TestResult result = performTests();
            String reportJson = OBJECT_MAPPER.writeValueAsString(result.getLambdaReport());
            LOGGER.info("Generated report:\n{}", reportJson);
            return createResponse(200, reportJson);
        } catch (Exception e) {
            LOGGER.error("Test execution failed", e);
            return createResponse(500, formatErrorResponse(e));
        }
    }

    private static SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    private void initializeKeystore(SecretsGrabber secretsGrabber) throws IOException {
        Keytool.createKeyStoreFile(
                Config.JKS_FILE_LOCATION, secretsGrabber.getSecretValue(Config.KEYSTORE_SECRET));
    }

    private TestResult performTests() {
        LOGGER.info("Starting tests with WASP URL: {}", waspUrl);

        // Test keystore contents
        String keyStoreContent =
                Keytool.getKeyStoreContents(Config.JKS_FILE_LOCATION, keystorePassword);
        LOGGER.info("Keystore contents:\n{}", keyStoreContent);

        // Test certificate import
        String importResult =
                Keytool.importCertificate(
                        Config.JKS_FILE_LOCATION, Config.PFX_FILE_LOCATION, keystorePassword);
        boolean certImported = importResult.contains("successfully imported");
        LOGGER.info(
                "Certificate import {}: {}", certImported ? "successful" : "failed", importResult);

        // Test SSL handshake
        SSLHandshakeReport sslReport;
        try {
            sslReport = SSLConnection.testSSLHandshake(Config.WASP_HOST, Config.WASP_PORT);
        } catch (SSLPeerUnverifiedException e) {
            sslReport = new SSLHandshakeReport();
            sslReport.setSessionValid(false);
        }
        LOGGER.info("SSL handshake {}!", sslReport.isSessionValid() ? "successful" : "failed");

        // Test SOAP request
        LoginWithCertificateReport soapReport =
                LoginWithCertificate.performRequest(
                        Config.PFX_FILE_LOCATION, keystorePassword, waspUrl);
        LOGGER.info("SOAP request status: HTTP {}", soapReport.getStatusCode());

        // Build assertions
        Assertions assertions = new Assertions();
        assertions.setSslConnection(status(sslReport.isSessionValid()));
        assertions.setSoapTokenRequest(status(soapReport.getStatusCode() == 200));
        assertions.setKeystoreImport(status(certImported));
        assertions.setJksLoaded(status(new File(Config.JKS_FILE_LOCATION).exists()));
        assertions.setSoapTokenValid(status(soapReport.isSoapTokenValid()));

        return new TestResult(new LambdaReport(sslReport, soapReport, assertions));
    }

    private static String status(boolean condition) {
        return condition ? "success" : "failed";
    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(body);
    }

    private static String formatErrorResponse(Exception e) {
        return String.format("{\"error\": \"%s\"}", e.getMessage());
    }
}
