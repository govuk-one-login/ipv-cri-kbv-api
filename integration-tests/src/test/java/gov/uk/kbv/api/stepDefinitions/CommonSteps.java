package gov.uk.kbv.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.uk.kbv.api.client.ClientConfigurationService;
import gov.uk.kbv.api.client.CommonApiClient;
import gov.uk.kbv.api.client.IpvCoreStubClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommonSteps {
    private final ObjectMapper objectMapper;
    private final CommonApiClient commonApiClient;
    private final IpvCoreStubClient ipvCoreStubClient;
    private final CriTestContext testContext;

    private String sessionRequestBody;
    private String authorizationCode;

    public CommonSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.commonApiClient = new CommonApiClient(clientConfigurationService);
        this.ipvCoreStubClient = new IpvCoreStubClient(clientConfigurationService);
        this.objectMapper = new ObjectMapper();
        this.testContext = testContext;
    }

    @Given("user has the user identity in the form of a signed JWT string")
    public void userHasTheUserIdentityInTheFormOfASignedJWTString()
            throws URISyntaxException, IOException, InterruptedException {
        int testUserDataSheetRowNumber = 681;
        this.testContext.setSerialisedUserIdentity(
                this.ipvCoreStubClient.getClaimsForUser(testUserDataSheetRowNumber));
        sessionRequestBody =
                this.ipvCoreStubClient.createSessionRequest(
                        this.testContext.getSerialisedUserIdentity());
    }

    @When("user sends a POST request to session end point")
    public void user_sends_a_post_request_to_session_end_point()
            throws IOException, InterruptedException, URISyntaxException {

        this.testContext.setResponse(this.commonApiClient.sendSessionRequest(sessionRequestBody));
        Map<String, String> deserializedResponse =
                objectMapper.readValue(
                        this.testContext.getResponse().body(), new TypeReference<>() {});
        this.testContext.setSessionId(deserializedResponse.get("session_id"));
    }

    @When("user sends a GET request to authorization end point")
    public void user_sends_a_get_request_to_authorization_end_point()
            throws IOException, URISyntaxException, InterruptedException {
        this.testContext.setResponse(
                this.commonApiClient.sendAuthorizationRequest(this.testContext.getSessionId()));
    }

    @When("user sends a POST request to token end point")
    public void user_sends_a_post_request_to_token_end_point()
            throws URISyntaxException, IOException, InterruptedException {
        String privateKeyJWT =
                this.ipvCoreStubClient.getPrivateKeyJWTFormParamsForAuthCode(
                        authorizationCode.trim());
        this.testContext.setResponse(this.commonApiClient.sendTokenRequest(privateKeyJWT));
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        assertNotNull(this.testContext.getSessionId());
    }

    @Then("user gets status code {int}")
    public void user_gets_status_code(Integer statusCode) {
        assertEquals(statusCode, this.testContext.getResponse().statusCode());
    }

    @And("a valid authorization code is returned in the response")
    public void aValidAuthorizationCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertNotNull(authorizationCode);
    }

    @And("a valid access token code is returned in the response")
    public void aValidAccessTokenCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        this.testContext.setAccessToken(jsonNode.get("access_token").asText());
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        assertFalse(this.testContext.getAccessToken().isEmpty());
    }
}
