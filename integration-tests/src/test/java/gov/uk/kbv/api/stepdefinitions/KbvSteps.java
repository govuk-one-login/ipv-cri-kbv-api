package gov.uk.kbv.api.stepdefinitions;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.kbv.api.client.KbvApiClient;
import gov.uk.kbv.api.util.SQSHelper;
import gov.uk.kbv.api.util.StackProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Timeout;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KbvSteps {
    private final ObjectMapper objectMapper;
    private final KbvApiClient kbvApiClient;
    private final CriTestContext testContext;

    private String questionId;

    private final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();

    private final String txmaQueueUrl =
            StackProperties.getOutput(
                    StackProperties.getParameter(System.getenv("STACK_NAME"), "CommonStackName"),
                    "MockAuditEventQueueUrl");

    public KbvSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
        this.testContext = testContext;
    }

    @When("user sends a GET request to question end point")
    public void user_sends_a_get_request_to_question_end_point()
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(this.testContext.getSessionId()));
        var deserializeGetResponse =
                objectMapper.readValue(this.testContext.getResponse().body(), KbvQuestion.class);
        makeQuestionAssertions(deserializeGetResponse);
    }

    @When("user sends a GET request to question end point when there are no questions left")
    public void userSendsAGETRequestToQuestionEndPointWhenThereAreNoQuestionsLeft()
            throws IOException, InterruptedException {
        testContext.setResponse(
                this.kbvApiClient.sendQuestionRequest(this.testContext.getSessionId()));
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
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
    public void user_answers_the_question_incorrectly() throws IOException, InterruptedException {
        this.kbvApiClient.submitIncorrectAnswers(questionId, this.testContext.getSessionId());
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        String responseBody = this.testContext.getResponse().body();
        assertNotNull(responseBody);
        makeVerifiableCredentialJwtAssertions(SignedJWT.parse(responseBody));
    }

    private void makeQuestionAssertions(KbvQuestion kbvQuestion) {
        if (kbvQuestion != null) {
            assertNotNull(kbvQuestion.getText());
            assertNotNull(kbvQuestion.getQuestionId());
            questionId = kbvQuestion.getQuestionId();
        }
    }

    private void makeVerifiableCredentialJwtAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        JsonNode userIdentity = objectMapper.readTree(this.testContext.getSerialisedUserIdentity());

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);
        assertNotNull(payload);
        assertNotNull(payload.get("nbf"));

        // assertNotNull(payload.get("exp"));
        // long expectedJwtTtl = 2L * 60L * 60L;
        // assertEquals(expectedJwtTtl, payload.get("exp").asLong() - payload.get("nbf").asLong());

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

    @And("a verification score of {int} is returned in the response")
    public void aVerificationScoreIsReturnedInTheResponse(int score)
            throws ParseException, IOException {
        String responseBody = this.testContext.getResponse().body();
        SignedJWT decodedJWT = SignedJWT.parse(responseBody);
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        assertEquals(
                score, payload.get("vc").get("evidence").get(0).get("verificationScore").asInt());
    }

    @Then("TXMA event is added to the sqs queue containing header value")
    public void txma_event_is_added_to_the_sqs_queue()
            throws InterruptedException, JsonProcessingException {
        final Map<String, String> eventProperties = new HashMap<>();
        eventProperties.put("/event_name", "IPV_KBV_CRI_START");
        eventProperties.put("/user/session_id", testContext.getSessionId());

        final List<Message> startEventMessages =
                SQSHelper.receiveMatchingMessages(txmaQueueUrl, 1, eventProperties, false);

        assertEquals(1, startEventMessages.size());

        final String deviceInformationHeader =
                objectMapper
                        .readTree(startEventMessages.get(0).getBody())
                        .path("restricted")
                        .path("device_information")
                        .path("encoded")
                        .asText();

        assertNotNull(deviceInformationHeader);
        assertNotEquals("", deviceInformationHeader);

        //        DeleteMessageBatchRequest batch =
        //                new DeleteMessageBatchRequest().withQueueUrl(txmaQueueUrl);
        //        List<DeleteMessageBatchRequestEntry> entries = batch.getEntries();
        //
        //        startEventMessages.forEach(
        //                m ->
        //                        entries.add(
        //                                new DeleteMessageBatchRequestEntry()
        //                                        .withId(m.getMessageId())
        //                                        .withReceiptHandle(m.getReceiptHandle())));
        //        sqsClient.deleteMessageBatch(batch);
    }

    @Then("TXMA event is added to the sqs queue not containing header value")
    public void txmaEventIsAddedToTheSqsQueueNotContainingHeaderValue() throws Exception {
        final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest()
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(txmaQueueUrl)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(100);

        ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);

        List<Message> sqsMessageList = null;
        if (receiveMessageResult != null
                && receiveMessageResult.getMessages() != null
                && receiveMessageResult.getMessages().size() > 0) {
            sqsMessageList = receiveMessageResult.getMessages();
            int startEventCounter = 0;
            for (Message sqsMessage : sqsMessageList) {
                String receivedMessageBody = sqsMessage.getBody();
                if (receivedMessageBody.contains("IPV_KBV_CRI_START")) {
                    assertFalse(receivedMessageBody.contains("device_information"));
                    startEventCounter++;
                } else System.out.println("START event not found");
            }
            if (startEventCounter == 0) {
                throw new Exception("None of the messages contain IPV_KBV_CRI_START");
            }
        } else throw new Exception("RecieveMessageResult is empty");

        DeleteMessageBatchRequest batch =
                new DeleteMessageBatchRequest().withQueueUrl(txmaQueueUrl);
        List<DeleteMessageBatchRequestEntry> entries = batch.getEntries();

        sqsMessageList.forEach(
                m ->
                        entries.add(
                                new DeleteMessageBatchRequestEntry()
                                        .withId(m.getMessageId())
                                        .withReceiptHandle(m.getReceiptHandle())));
        sqsClient.deleteMessageBatch(batch);
    }

    @And("the SQS events are purged from the queue without wait")
    public void the_sqs_events_are_purged_from_the_queue_without_wait()
            throws InterruptedException {
        Thread.sleep(30000);
        PurgeQueueRequest pqRequest = new PurgeQueueRequest(txmaQueueUrl);
        sqsClient.purgeQueue(pqRequest);
        Thread.sleep(30000);
    }

    @And("the SQS events are purged from the queue")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    public void the_sqs_events_are_purged_from_the_queue() throws InterruptedException {
        Thread.sleep(40000);
        PurgeQueueRequest pqRequest = new PurgeQueueRequest(txmaQueueUrl);
        sqsClient.purgeQueue(pqRequest);
    }

    @And("the SQS events are deleted from the queue")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    public void the_sqs_events_are_deleted_from_the_queue() throws InterruptedException {
        SQSHelper.deleteMatchingMessages(
                txmaQueueUrl,
                9,
                Collections.singletonMap("/user/session_id", testContext.getSessionId()));
    }
}
