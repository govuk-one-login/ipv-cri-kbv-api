package gov.uk.kbv.api.stepdefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.kbv.api.client.KbvApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.TestHarnessResponse;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;
import uk.gov.di.ipv.cri.common.library.util.JsonSchemaValidator;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class KbvSteps {

    private final ObjectMapper objectMapper;
    private final KbvApiClient kbvApiClient;
    private final CriTestContext testContext;

    private String questionId;

    private static final String KBV_START_SCHEMA_FILE = "/features/schema/IPV_KBV_CRI_START.json";
    private static final String KBV_RESPONSE_RECEIVED_SCHEMA_FILE =
            "/features/schema/IPV_KBV_CRI_RESPONSE_RECEIVED.json";

    private final String kbvStartJsonSchema;
    private final String kbvResponseReceivedJsonSchema;

    public KbvSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext)
            throws IOException, URISyntaxException {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
        this.testContext = testContext;

        Path startSchemaFile =
                Paths.get(
                        Objects.requireNonNull(KbvSteps.class.getResource(KBV_START_SCHEMA_FILE))
                                .toURI());
        this.kbvStartJsonSchema = Files.readString(startSchemaFile);

        Path responseReceivedSchemaFile =
                Paths.get(
                        Objects.requireNonNull(
                                        KbvSteps.class.getResource(
                                                KBV_RESPONSE_RECEIVED_SCHEMA_FILE))
                                .toURI());
        this.kbvResponseReceivedJsonSchema = Files.readString(responseReceivedSchemaFile);
    }

    @When("user sends a GET request to question endpoint")
    public void userSendsGetRequestToQuestionEndpoint() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(this.testContext.getSessionId()));

        final var kbvQuestion =
                objectMapper.readValue(this.testContext.getResponse().body(), KbvQuestion.class);

        assertNotNull(kbvQuestion);
        assertNotNull(kbvQuestion.getText());
        assertNotNull(kbvQuestion.getQuestionId());

        this.questionId = kbvQuestion.getQuestionId();
    }

    @When("user sends a GET request to question endpoint when there are no questions left")
    public void userSendsGetRequestToQuestionEndpointWhenThereAreNoQuestionsLeft()
            throws IOException, InterruptedException {
        testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(this.testContext.getSessionId()));
    }

    @When("user sends a POST request to credential issue endpoint with a valid access token")
    public void userSendsPostRequestToCredentialIssueEndpointWithValidAccessToken()
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendIssueCredentialRequest(this.testContext.getAccessToken()));
    }

    @When("user chooses to abandon the question")
    public void userChoosesToAbandonTheQuestion() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendAbandonRequest(this.testContext.getSessionId()));
    }

    @And("{int} answers the question correctly")
    public void userAnswersTheQuestionCorrectly(int user) throws IOException, InterruptedException {
        this.kbvApiClient.submitCorrectAnswers(questionId, this.testContext.getSessionId(), user);
    }

    @Then("{int} answers the question incorrectly")
    public void userAnswersTheQuestionIncorrectly(int user)
            throws IOException, InterruptedException {
        this.kbvApiClient.submitIncorrectAnswers(questionId, this.testContext.getSessionId(), user);
    }

    @And("a valid JWT is returned in the response")
    public void validJwtIsReturnedInTheResponse() throws ParseException, IOException {
        final String responseBody = this.testContext.getResponse().body();

        assertNotNull(responseBody);
        makeVerifiableCredentialJwtAssertions(SignedJWT.parse(responseBody));
    }

    @And("a verification score of {int} is returned in the response")
    public void verificationScoreIsReturnedInTheResponse(int score)
            throws ParseException, IOException {
        final SignedJWT decodedJWT = SignedJWT.parse(this.testContext.getResponse().body());
        final var payload = objectMapper.readTree(decodedJWT.getPayload().toString());

        assertEquals(score, payload.at("/vc/evidence/0/verificationScore").asInt());
    }

    @And("the check details array has {int} objects returned in the response")
    public void checkDetailsIsReturnedInTheResponse(int object) throws ParseException, IOException {
        final SignedJWT decodedJWT = SignedJWT.parse(this.testContext.getResponse().body());
        final var payload = objectMapper.readTree(decodedJWT.getPayload().toString());

        assertEquals(object, payload.at("/vc/evidence/0/checkDetails").size());
    }

    @And("the failed details array has {int} objects returned in the response")
    public void failedDetailsIsReturnedInTheResponse(int object)
            throws ParseException, IOException {
        final SignedJWT decodedJWT = SignedJWT.parse(this.testContext.getResponse().body());
        final var payload = objectMapper.readTree(decodedJWT.getPayload().toString());

        assertEquals(object, payload.at("/vc/evidence/0/failedCheckDetails").size());
    }

    @And("a valid START event is returned in the response with txma header")
    public void aValidStartEventIsReturnedInTheResponseWithTxmaHeader() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();
        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> events =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> response : events) {
            AuditEvent<?> event = response.readAuditEvent();
            assertEquals("IPV_KBV_CRI_START", event.getEvent());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertEquals(
                    "deviceInformation", event.getRestricted().getDeviceInformation().getEncoded());
        }
    }

    @And("a valid START event is returned in the response without txma header")
    public void aValidStartEventIsReturnedInTheResponseWithoutTxmaHeader() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();
        assertEquals(200, testContext.getTestHarnessResponse().httpResponse().statusCode());
        assertNotNull(responseBody);

        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> events =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> response : events) {
            AuditEvent<?> event = response.readAuditEvent();
            assertEquals("IPV_KBV_CRI_START", event.getEvent());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            response.getEvent().getData(), kbvStartJsonSchema));
            assertNull(event.getRestricted());
        }
    }

    @Then("START TxMA event is validated against schema")
    public void startTxmaEventValidatedAgainstSchema() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();

        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> testHarnessResponses =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        var events =
                testHarnessResponses.stream()
                        .filter(event -> event.getEvent().toString().equals("IPV_KBV_CRI_START"))
                        .collect(Collectors.toList());

        assertNotNull(events);
        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> testHarnessResponse : events) {
            AuditEvent<?> event =
                    objectMapper.readValue(
                            testHarnessResponse.getEvent().getData(), AuditEvent.class);
            assertEquals(1, events.size());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            testHarnessResponse.getEvent().getData(), kbvStartJsonSchema));
        }
    }

    @And("a RESPONSE_RECEIVED event is returned with repeatAttemptAlert present {word}")
    public void aResponseReceivedEventIsReturnedWithRepeatAttemptedAlert(String attemptAlert)
            throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();
        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> events =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> response : events) {
            AuditEvent<?> event = response.readAuditEvent();

            assertEquals("IPV_KBV_CRI_RESPONSE_RECEIVED", event.getEvent());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());

            Object extensions = event.getExtensions();
            String jsonString = objectMapper.writeValueAsString(extensions);
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            boolean repeatAttemptAlert =
                    jsonNode.path("experianIiqResponse").path("repeatAttemptAlert").asBoolean();
            assertEquals(Boolean.valueOf(attemptAlert), repeatAttemptAlert);
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            response.getEvent().getData(), kbvResponseReceivedJsonSchema));
        }
    }

    private void makeVerifiableCredentialJwtAssertions(SignedJWT decodedJWT) throws IOException {
        final var header = objectMapper.readTree(decodedJWT.getHeader().toString());
        final var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        final var userIdentity =
                objectMapper.readTree(this.testContext.getSerialisedUserIdentity());

        assertEquals("JWT", header.at("/typ").asText());
        assertEquals("ES256", header.at("/alg").asText());

        assertEquals(
                "did:web:review-k.dev.account.gov.uk#f06e603c0b11557151851ba196a46657f47daaca9f151761980f9f5c39210482",
                header.at("/kid").asText());

        assertNotNull(payload);
        assertNotNull(payload.at("/nbf"));
        assertNotNull(payload.at("/vc/evidence/0/txn"));

        assertNotEquals("", payload.at("/nbf").asText());
        assertNotEquals("", payload.at("/vc/evidence/0/txn").asText());

        assertEquals("IdentityCheck", payload.at("/vc/evidence/0/type").asText());
        assertEquals("VerifiableCredential", payload.at("/vc/type/0").asText());
        assertEquals("IdentityCheckCredential", payload.at("/vc/type/1").asText());

        assertEquals(
                userIdentity.at("/shared_claims/birthDate/0/value").asText(),
                payload.at("/vc/credentialSubject/birthDate/0/value").asText());

        assertEquals(
                userIdentity.at("/shared_claims/name/0/nameParts/0/value").asText(),
                payload.at("/vc/credentialSubject/name/0/nameParts/0/value").asText());
    }
}
