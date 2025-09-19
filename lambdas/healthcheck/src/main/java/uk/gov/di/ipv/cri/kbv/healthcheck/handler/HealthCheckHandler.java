package uk.gov.di.ipv.cri.kbv.healthcheck.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.util.TempCleaner;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.certificate.KeyToolAssertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.keystore.KeyStoreAssertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.overview.GeneralOverviewAssertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap.SOAPRequestAssertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.ssl.SSLHandshakeAssertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.config.Configuration;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.config.ExperianSecrets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthCheckHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INTERNAL_SERVER_ERR = "Internal Server Error";

    private final ExperianSecrets experianSecrets;

    @ExcludeFromGeneratedCoverageReport
    public HealthCheckHandler() {
        TempCleaner.clean();
        this.experianSecrets = new ExperianSecrets();
    }

    public HealthCheckHandler(ExperianSecrets experianSecrets) {
        this.experianSecrets = experianSecrets;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            boolean verboseOutput =
                    request.getPath() != null && request.getPath().endsWith("/info");

            String keystorePassword = experianSecrets.getKeystorePassword();
            String keystoreSecret = experianSecrets.getKeystoreSecret();
            String waspURL = experianSecrets.getWaspUrl();

            Map<String, Report> reports = runAssertions(keystoreSecret, keystorePassword, waspURL);

            Report generalOverviewReport = new GeneralOverviewAssertion(reports).run();
            reports.put("Overview", generalOverviewReport);

            response.setStatusCode(generalOverviewReport.isPassed() ? 200 : 400);
            response.setBody(
                    verboseOutput
                            ? OBJECT_MAPPER
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(reports)
                            : "{}");

        } catch (Exception e) {
            LOGGER.error("An error occurred while handling request", e);
            response.setStatusCode(500);
            response.setBody(INTERNAL_SERVER_ERR);
        }

        return response;
    }

    private Map<String, Report> runAssertions(
            String keystoreSecret, String keystorePassword, String waspURL) throws IOException {
        Map<String, Report> reports = new HashMap<>();

        List<Assertion> tests =
                List.of(
                        new SSLHandshakeAssertion(
                                experianSecrets.getWaspUrl(), Configuration.WASP_PORT),
                        new KeyToolAssertion(keystoreSecret, keystorePassword),
                        new KeyStoreAssertion(keystoreSecret, keystorePassword),
                        new SOAPRequestAssertion(
                                keystorePassword,
                                waspURL,
                                keystoreSecret)); // Run SOAPRequestAssertion Last!

        for (Assertion assertion : tests) {
            String name = assertion.getClass().getSimpleName();
            try {
                LOGGER.info("Running assertion: {}", name);
                long startTime = System.currentTimeMillis();
                reports.put(name, assertion.run());
                long endTime = System.currentTimeMillis() - startTime;
                LOGGER.info("Completed assertion: {} Time: {}ms", name, endTime);
            } catch (Exception e) {
                LOGGER.error("Failed to run assertion: {}", name, e);
                reports.put(name, new FailReport(e));
            }
        }

        return reports;
    }
}
