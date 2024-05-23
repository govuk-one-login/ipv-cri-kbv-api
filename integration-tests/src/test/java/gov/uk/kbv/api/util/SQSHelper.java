package gov.uk.kbv.api.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SQSHelper {

    public static final int SQS_TIMEOUT_SECONDS = 30;
    public static final int SQS_MESSAGE_VISIBILITY_TIMEOUT = 10;

    private static final int SQS_BATCH_SIZE = 10;

    private static final AmazonSQS SQS_CLIENT =
            AmazonSQSClientBuilder.standard().withRegion(System.getenv("AWS_REGION")).build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Message> receiveMessages(String queueUrl, int count)
            throws InterruptedException {
        return receiveMessages(queueUrl, count, null, false, SQS_TIMEOUT_SECONDS);
    }

    public static List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> properties, boolean deleteNonMatching)
            throws InterruptedException {
        return receiveMessages(queueUrl, count, properties, deleteNonMatching, SQS_TIMEOUT_SECONDS);
    }

    //    public static List<Message> receiveMessages(
    //            String queueUrl, Map<String, String> properties, int desiredCount)
    //            throws InterruptedException {
    //        return receiveMessages(
    //                queueUrl, properties, desiredCount, false, SQS_TIMEOUT_SECONDS);
    //    }
    //
    //    public static List<Message> receiveMessages(
    //            String queueUrl,
    //            Map<String, String> properties,
    //            int count,
    //            boolean deleteNonMatching)
    //            throws InterruptedException {
    //        return receiveMessages(
    //                queueUrl, properties, count, deleteNonMatching, SQS_TIMEOUT_SECONDS);
    //    }

    private static List<Message> receiveMessages(
            String queueUrl,
            int count,
            Map<String, String> filters,
            boolean deleteNonMatching,
            int timeoutSeconds)
            throws InterruptedException {
        List<Message> allMessages = new ArrayList<>();
        List<Message> targetMessages = new ArrayList<>();

        final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest(queueUrl)
                        .withMaxNumberOfMessages(10)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(SQS_MESSAGE_VISIBILITY_TIMEOUT);

        final boolean filterMessages = filters != null;
        final long startTime = System.currentTimeMillis();

        while (targetMessages.size() < count) {
            final List<Message> newMessages =
                    SQS_CLIENT.receiveMessage(receiveMessageRequest).getMessages();
            allMessages.addAll(newMessages);

            if (filterMessages) {
                targetMessages.addAll(
                        newMessages.stream()
                                .filter(message -> messageMatches(message, filters))
                                .collect(Collectors.toList()));
            } else {
                targetMessages = allMessages;
            }

            if (targetMessages.size() < count) {
                Thread.sleep(500);
            }

            if (System.currentTimeMillis() - startTime >= timeoutSeconds * 1000L) {
                throw new RuntimeException(
                        String.format(
                                "Received %d/%d messages after %d seconds",
                                targetMessages.size(), count, timeoutSeconds));
            }
        }

        final List<Message> matchingMessages = targetMessages;

        final List<Message> nonMatchingMessages =
                allMessages.stream()
                        .filter(message -> !matchingMessages.contains(message))
                        .collect(Collectors.toList());

        if (filterMessages && deleteNonMatching) {
            deleteMessages(queueUrl, nonMatchingMessages);
            resetMessageVisibilityTimeoutBatch(queueUrl, matchingMessages);
        } else {
//            resetMessageVisibilityTimeoutBatch(queueUrl, allMessages);
        }

        return matchingMessages;
    }

    public static void deleteMatchingMessages(
            String queueUrl, int count, Map<String, String> properties)
            throws InterruptedException {
        final List<Message> messages =
                receiveMessages(queueUrl, count, properties, false, SQS_TIMEOUT_SECONDS);
        deleteMessages(queueUrl, messages);
    }

    public static void deleteMessages(String queueUrl, int count) throws InterruptedException {
        deleteMessages(queueUrl, count, SQS_TIMEOUT_SECONDS);
    }

    public static void deleteMessages(String queueUrl, int count, int timeoutSeconds)
            throws InterruptedException {
        final List<Message> messages =
                receiveMessages(queueUrl, count, null, false, timeoutSeconds);
        deleteMessages(queueUrl, messages);
    }

    public static void deleteMessages(String queueUrl, List<Message> messages) {
        SQS_CLIENT.deleteMessageBatch(
                queueUrl,
                messages.stream()
                        .map(
                                message ->
                                        new DeleteMessageBatchRequestEntry()
                                                .withId(message.getMessageId())
                                                .withReceiptHandle(message.getReceiptHandle()))
                        .collect(Collectors.toList()));
    }

    private static JsonNode parseJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static boolean messageMatches(Message message, Map<String, String> properties) {
        final JsonNode body = Objects.requireNonNull(parseJson(message.getBody()));
        return properties.keySet().stream()
                .allMatch(key -> body.at(key).asText().equals(properties.get(key)));
    }

    private static void resetMessageVisibilityTimeoutBatch(
            String queueUrl, List<Message> messages) {
        while (!messages.isEmpty()) {
            final List<Message> messageBatch =
                    messages.subList(0, Math.min(SQS_BATCH_SIZE, messages.size()));

            SQS_CLIENT.changeMessageVisibilityBatch(
                    queueUrl,
                    messageBatch.stream()
                            .map(
                                    message ->
                                            new ChangeMessageVisibilityBatchRequestEntry()
                                                    .withId(message.getMessageId())
                                                    .withReceiptHandle(message.getReceiptHandle())
                                                    .withVisibilityTimeout(0))
                            .collect(Collectors.toList()));

            messages.removeAll(messageBatch);
        }

        //        for (int idx = 0; idx < messages.size(); idx += SQS_BATCH_SIZE) {
        //            SQS_CLIENT.changeMessageVisibilityBatch(
        //                    queueUrl,
        //                    messages.subList(idx, Math.min(idx + SQS_BATCH_SIZE,
        // messages.size())).stream()
        //                            .map(
        //                                    message ->
        //                                            new ChangeMessageVisibilityBatchRequestEntry()
        //                                                    .withId(message.getMessageId())
        //
        // .withReceiptHandle(message.getReceiptHandle())
        //                                                    .withVisibilityTimeout(0))
        //                            .collect(Collectors.toList()));
        //        }
    }
}
