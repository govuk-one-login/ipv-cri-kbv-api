package gov.uk.kbv.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.kbv.api.client.ClientConfigurationService;
import gov.uk.kbv.api.client.CommonApiClient;
import gov.uk.kbv.api.client.IpvCoreStubClient;
import gov.uk.kbv.api.client.KbvApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class APISteps {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CommonApiClient commonApiClient;
    private final IpvCoreStubClient ipvCoreStubClient;
    private final KbvApiClient kbvApiClient;
    private String sessionRequestBody;
    private String sessionId;
    private String questionId;
    private String authorizationCode;
    private String accessToken;
    private String userIdentityJson;
    private HttpResponse<String> response;

    public APISteps() {
        ClientConfigurationService clientConfigurationService =
                new ClientConfigurationService("dev");
        this.commonApiClient = new CommonApiClient(clientConfigurationService);
        this.ipvCoreStubClient = new IpvCoreStubClient(clientConfigurationService);
        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
    }

    @Given("user has the user identity in the form of a signed JWT string")
    public void userHasTheUserIdentityInTheFormOfASignedJWTString()
            throws URISyntaxException, IOException, InterruptedException {
        int testUserDataSheetRowNumber = 681;
        userIdentityJson = this.ipvCoreStubClient.getClaimsForUser(testUserDataSheetRowNumber);
        sessionRequestBody = this.ipvCoreStubClient.createSessionRequest(userIdentityJson);
    }

    @When("user sends a POST request to session end point")
    public void user_sends_a_post_request_to_session_end_point()
            throws IOException, InterruptedException, URISyntaxException {

        response = this.commonApiClient.sendSessionRequest(sessionRequestBody);
        Map<String, String> deserializedResponse =
                objectMapper.readValue(this.response.body(), new TypeReference<>() {});
        sessionId = deserializedResponse.get("session_id");
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        assertNotNull(sessionId);
    }

    @When("user sends a GET request to question end point")
    public void user_sends_a_get_request_to_question_end_point()
            throws IOException, URISyntaxException, InterruptedException {
        response = this.kbvApiClient.sendQuestionRequest(sessionId);
        Map<String, String> deserializeGetResponse =
                objectMapper.readValue(response.body(), new TypeReference<>() {});
        makeQuestionAssertions(deserializeGetResponse);
    }

    private void makeQuestionAssertions(Map<String, String> deserialisedGETResponse) {
        if (!deserialisedGETResponse.isEmpty()) {
            assertNotNull(deserialisedGETResponse.get("text"));
            assertNotNull(deserialisedGETResponse.get("questionID"));
            questionId = deserialisedGETResponse.get("questionID");
        }
    }

    @And("user answers the question correctly")
    public void userAnswersTheQuestionCorrectly()
            throws IOException, URISyntaxException, InterruptedException {
        this.kbvApiClient.submitCorrectAnswers(questionId, sessionId);
    }

    @When("user sends a GET request to question end point when there are no questions left")
    public void userSendsAGETRequestToQuestionEndPointWhenThereAreNoQuestionsLeft()
            throws IOException, InterruptedException, URISyntaxException {
        response = this.kbvApiClient.sendQuestionRequest(sessionId);
    }

    @Then("user gets status code {int}")
    public void user_gets_status_code(Integer statusCode) {
        assertEquals(statusCode, response.statusCode());
    }

    @When("user sends a GET request to authorization end point")
    public void user_sends_a_get_request_to_authorization_end_point()
            throws IOException, URISyntaxException, InterruptedException {
        response = this.commonApiClient.sendAuthorizationRequest(sessionId);
    }

    @And("a valid authorization code is returned in the response")
    public void aValidAuthorizationCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertNotNull(authorizationCode);
    }

    @When("user sends a POST request to token end point")
    public void user_sends_a_post_request_to_token_end_point()
            throws URISyntaxException, IOException, InterruptedException {
        String privateKeyJWT =
                this.ipvCoreStubClient.getPrivateKeyJWTFormParamsForAuthCode(
                        authorizationCode.trim());
        response = this.commonApiClient.sendTokenRequest(privateKeyJWT);
    }

    @And("a valid access token code is returned in the response")
    public void aValidAccessTokenCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        accessToken = jsonNode.get("access_token").asText();
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        assertFalse(accessToken.isEmpty());
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
            throws IOException, InterruptedException, URISyntaxException {
        response = this.kbvApiClient.sendIssueCredentialRequest(accessToken);
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        assertNotNull(response.body());
        makeVerifiableCredentialJwtAssertions(SignedJWT.parse(response.body()));
    }

    private void makeVerifiableCredentialJwtAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        JsonNode identityJSON = objectMapper.readTree(userIdentityJson);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);
        assertNotNull(payload);
        assertEquals(2, payload.get("vc").get("evidence").get(0).get("verificationScore").asInt());
        assertNotNull(payload.get("nbf"));
        assertNotNull(payload.get("exp"));
        assertEquals(
                "IdentityCheck", payload.get("vc").get("evidence").get(0).get("type").asText());
        assertNotNull(payload.get("vc").get("evidence").get(0).get("txn").asText());
        assertEquals("VerifiableCredential", payload.get("vc").get("type").get(0).asText());
        assertEquals("IdentityCheckCredential", payload.get("vc").get("type").get(1).asText());
        assertEquals(
                payload.get("vc")
                        .get("credentialSubject")
                        .get("birthDate")
                        .get(0)
                        .get("value")
                        .asText(),
                identityJSON.get("shared_claims").get("birthDate").get(0).get("value").asText());
        assertEquals(
                payload.get("vc")
                        .get("credentialSubject")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText(),
                identityJSON
                        .get("shared_claims")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText());
    }

    @When("user chooses to abandon the question")
    public void userChoosesToAbandonTheQuestion()
            throws URISyntaxException, IOException, InterruptedException {
        response = this.kbvApiClient.sendAbandonRequest(sessionId);
    }

    @And("JWT time-to-live is {long} hours")
    public void jwt_lives_for_appropriate_time(long hours) throws ParseException, IOException {
        SignedJWT decodedJWT = SignedJWT.parse(response.body());
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        long ttl = hours * 60 * 60;
        assertEquals(ttl, payload.get("exp").asLong() - payload.get("nbf").asLong());
    }
}
