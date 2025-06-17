package gov.uk.kbv.api.stepdefinitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HealthCheckEndpointStepDefinition {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ClientConfigurationService clientConfigurationService;
    private final String healthCheckEndpoint;

    private HttpResponse<String> response;

    public HealthCheckEndpointStepDefinition(ClientConfigurationService config) {
        String baseURL = config.getPublicApiEndpoint();
        String environment = System.getenv("ENVIRONMENT");

        this.clientConfigurationService = config;
        this.healthCheckEndpoint = "%s/%s/healthcheck/thirdparty".formatted(baseURL, environment);
    }

    @Given("user visits the verbose health check endpoint")
    public void userVisitsTheVerboseHealthCheckEndpoint()
            throws URISyntaxException, IOException, InterruptedException {
        sendRequestToHealthCheckEndpoint(healthCheckEndpoint + "/info");
    }

    @Given("user visits the health check endpoint")
    public void userVisitsTheHealthCheckEndpoint()
            throws URISyntaxException, IOException, InterruptedException {
        sendRequestToHealthCheckEndpoint(healthCheckEndpoint);
    }

    @Then("they should see a detailed report of all the checks that were performed")
    public void theyShouldSeeADetailedReportOfAllTheChecksThatWerePerformed()
            throws JsonProcessingException {
        JsonNode body = OBJECT_MAPPER.readTree(response.body());
        assertNotNull(body.get("SSLHandshakeAssertion"));
        assertNotNull(body.get("Overview"));
        assertNotNull(body.get("SOAPRequestAssertion"));
        assertNotNull(body.get("KeyToolAssertion"));
        assertNotNull(body.get("KeyStoreAssertion"));
    }

    @And("the statuses of those test should be pass")
    public void theStatusesOfThoseTestShouldBePass() throws JsonProcessingException {
        JsonNode body = OBJECT_MAPPER.readTree(response.body());

        JsonNode sslHandshakeAssertion = body.get("SSLHandshakeAssertion");
        JsonNode overview = body.get("Overview");
        JsonNode soapRequestAssertion = body.get("SOAPRequestAssertion");
        JsonNode keyToolAssertion = body.get("KeyToolAssertion");
        JsonNode keyStoreAssertion = body.get("KeyStoreAssertion");

        assertTrue(sslHandshakeAssertion.get("passed").asBoolean());
        assertTrue(overview.get("passed").asBoolean());
        assertTrue(soapRequestAssertion.get("passed").asBoolean());
        assertTrue(keyToolAssertion.get("passed").asBoolean());
        assertTrue(keyStoreAssertion.get("passed").asBoolean());
    }

    @Then("they should shall not see an output")
    public void theyShouldShallNotSeeAnOutput() {
        assertEquals("{}".length(), response.body().length());
    }

    @And("the web page status should be {int}")
    public void theWebPageStatusShouldBe(int expectedStatusCode) {
        assertEquals(expectedStatusCode, response.statusCode());
    }

    private void sendRequestToHealthCheckEndpoint(String url)
            throws URISyntaxException, IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .uri(new URI(url))
                            .header(
                                    HttpHeaders.API_KEY,
                                    clientConfigurationService.getPublicApiKey())
                            .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertNotNull(response);
        }
    }
}
