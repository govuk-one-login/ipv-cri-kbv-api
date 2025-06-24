package gov.uk.kbv.api.stepdefinitions;

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
    private final ClientConfigurationService clientConfigurationService;
    private final String baseURL;

    private HttpResponse<String> response;

    public HealthCheckEndpointStepDefinition(ClientConfigurationService config) {
        this.clientConfigurationService = config;
        this.baseURL =
                "%s/%s".formatted(config.getPublicApiEndpoint(), System.getenv("ENVIRONMENT"));
    }

    @Given("user makes a request to {string} without being on the VPN")
    public void userMakesARequestToWithoutBeingOnTheVPN(String endpoint)
            throws URISyntaxException, IOException, InterruptedException {
        sendRequestToHealthCheckEndpoint(baseURL + endpoint);
    }

    @Then("they should see an access denied error")
    public void theyShouldSeeAnAccessDeniedError() {
        assertTrue(response.body().contains("anonymous is not authorized to perform"));
    }

    @And("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode());
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
