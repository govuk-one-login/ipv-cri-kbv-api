package gov.uk.kbv.api.stepdefinitions;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.kbv.api.client.KbvApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.gov.di.ipv.cri.common.library.aws.CloudFormationHelper;
import uk.gov.di.ipv.cri.common.library.aws.SQSHelper;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KbvSteps {
    private final ObjectMapper objectMapper;
    private final KbvApiClient kbvApiClient;
    private final CriTestContext testContext;
    private final SQSHelper sqs;
    private String questionId;

    private final String auditEventQueueUrl =
            CloudFormationHelper.getOutput(
                    CloudFormationHelper.getParameter(System.getenv("STACK_NAME"), "TxmaStackName"),
                    "AuditEventQueueUrl");

    public KbvSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
        this.testContext = testContext;

        this.sqs = new SQSHelper(null, this.objectMapper);
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

    @And("user answers the question correctly")
    public void userAnswersTheQuestionCorrectly() throws IOException, InterruptedException {
        this.kbvApiClient.submitCorrectAnswers(questionId, this.testContext.getSessionId());
    }

    @Then("user answers the question incorrectly")
    public void userAnswersTheQuestionIncorrectly() throws IOException, InterruptedException {
        this.kbvApiClient.submitIncorrectAnswers(questionId, this.testContext.getSessionId());
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

    @Then("TXMA event is added to the SQS queue containing device information header")
    public void txmaEventIsAddedToSqsQueueContainingDeviceInformationHeader()
            throws IOException, InterruptedException {
        assertEquals("deviceInformation", getDeviceInformationHeader());
    }

    @Then("TXMA event is added to the SQS queue containing evidence requested")
    public void txmaEventIsAddedToSqsQueueContainingEvidenceRequested()
            throws IOException, InterruptedException {
        assertEquals("1", getEvidenceRequested());
    }

    @Then("TXMA event is added to the SQS queue not containing device information header")
    public void txmaEventIsAddedToSqsQueueNotContainingDeviceInformationHeader()
            throws InterruptedException, IOException {
        assertEquals("", getDeviceInformationHeader());
    }

    @And("{int} events are deleted from the audit events SQS queue")
    public void deleteEventsFromSqsQueue(int messageCount) throws InterruptedException {
        this.sqs.deleteMatchingMessages(
                auditEventQueueUrl,
                messageCount,
                Collections.singletonMap("/user/session_id", testContext.getSessionId()));
    }

    private String getDeviceInformationHeader() throws InterruptedException, IOException {
        final List<Message> startEventMessages =
                this.sqs.receiveMatchingMessages(
                        auditEventQueueUrl,
                        1,
                        Map.ofEntries(
                                entry("/event_name", "IPV_KBV_CRI_START"),
                                entry("/user/session_id", testContext.getSessionId())));

        assertEquals(1, startEventMessages.size());

        return objectMapper
                .readTree(startEventMessages.get(0).body())
                .at("/restricted/device_information/encoded")
                .asText();
    }

    private String getEvidenceRequested() throws InterruptedException, IOException {
        final List<Message> startEventMessages =
                this.sqs.receiveMatchingMessages(
                        auditEventQueueUrl,
                        1,
                        Map.ofEntries(
                                entry("/event_name", "IPV_KBV_CRI_START"),
                                entry("/user/session_id", testContext.getSessionId())));

        assertEquals(1, startEventMessages.size());

        return objectMapper
                .readTree(startEventMessages.get(0).body())
                .at("/extensions/evidence_requested/verificationScore")
                .asText();
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
