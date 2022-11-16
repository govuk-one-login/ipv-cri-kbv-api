package gov.uk.kbv.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.kbv.api.client.ClientConfigurationService;
import gov.uk.kbv.api.client.KbvApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KbvSteps {
    private final ObjectMapper objectMapper;
    private final KbvApiClient kbvApiClient;
    private final CriTestContext testContext;

    private String questionId;

    public KbvSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.objectMapper = new ObjectMapper();
        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
        this.testContext = testContext;
    }

    @When("user sends a GET request to question end point")
    public void user_sends_a_get_request_to_question_end_point()
            throws IOException, URISyntaxException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(testContext.getSessionId()));
        Map<String, String> deserializeGetResponse =
                objectMapper.readValue(
                        this.testContext.getResponse().body(), new TypeReference<>() {});
        makeQuestionAssertions(deserializeGetResponse);
    }

    @When("user sends a GET request to question end point when there are no questions left")
    public void userSendsAGETRequestToQuestionEndPointWhenThereAreNoQuestionsLeft()
            throws IOException, InterruptedException, URISyntaxException {
        testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(this.testContext.getSessionId()));
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
            throws IOException, InterruptedException, URISyntaxException {
        this.testContext.setResponse(
                this.kbvApiClient.sendIssueCredentialRequest(this.testContext.getAccessToken()));
    }

    @When("user chooses to abandon the question")
    public void userChoosesToAbandonTheQuestion()
            throws URISyntaxException, IOException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendAbandonRequest(this.testContext.getSessionId()));
    }

    @And("user answers the question correctly")
    public void userAnswersTheQuestionCorrectly()
            throws IOException, URISyntaxException, InterruptedException {
        this.kbvApiClient.submitCorrectAnswers(questionId, this.testContext.getSessionId());
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        String responseBody = this.testContext.getResponse().body();
        assertNotNull(responseBody);
        makeVerifiableCredentialJwtAssertions(SignedJWT.parse(responseBody));
    }

    private void makeQuestionAssertions(Map<String, String> deserialisedGETResponse) {
        if (!deserialisedGETResponse.isEmpty()) {
            assertNotNull(deserialisedGETResponse.get("text"));
            assertNotNull(deserialisedGETResponse.get("questionID"));
            questionId = deserialisedGETResponse.get("questionID");
        }
    }

    private void makeVerifiableCredentialJwtAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        JsonNode userIdentity = objectMapper.readTree(this.testContext.getSerialisedUserIdentity());

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);
        assertNotNull(payload);
        assertEquals(2, payload.get("vc").get("evidence").get(0).get("verificationScore").asInt());
        assertNotNull(payload.get("nbf"));
        assertNotNull(payload.get("exp"));
        long expectedJwtTtl = 2L * 60L * 60L;
        assertEquals(expectedJwtTtl, payload.get("exp").asLong() - payload.get("nbf").asLong());
        assertEquals(
                "IdentityCheck", payload.get("vc").get("evidence").get(0).get("type").asText());
        assertNotNull(payload.get("vc").get("evidence").get(0).get("txn").asText());
        assertEquals("VerifiableCredential", payload.get("vc").get("type").get(0).asText());
        assertEquals("IdentityCheckCredential", payload.get("vc").get("type").get(1).asText());
        assertEquals(
                userIdentity.get("shared_claims").get("birthDate").get(0).get("value").asText(),
                payload.get("vc")
                        .get("credentialSubject")
                        .get("birthDate")
                        .get(0)
                        .get("value")
                        .asText());
        assertEquals(
                userIdentity
                        .get("shared_claims")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText(),
                payload.get("vc")
                        .get("credentialSubject")
                        .get("name")
                        .get(0)
                        .get("nameParts")
                        .get(0)
                        .get("value")
                        .asText());
    }
}
