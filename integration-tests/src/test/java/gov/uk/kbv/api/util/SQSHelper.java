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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SQSHelper {

    public static final int TIMEOUT_SECONDS = 30;
    public static final int SQS_WAIT_TIME_SECONDS = Math.min(20, TIMEOUT_SECONDS);
    public static final int SQS_MESSAGE_VISIBILITY_TIMEOUT =
            TIMEOUT_SECONDS + SQS_WAIT_TIME_SECONDS + 5;

    private static final AmazonSQS SQS_CLIENT =
            AmazonSQSClientBuilder.standard().withRegion(System.getenv("AWS_REGION")).build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Message> receiveMessages(String queueUrl, int count)
            throws InterruptedException {
        return receiveMatchingMessages(queueUrl, count, null, false);
    }

    public static List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters) throws InterruptedException {
        return receiveMatchingMessages(queueUrl, count, filters, false);
    }

    public static List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        return receiveMessages(queueUrl, count, filters, deleteNonMatching, false, TIMEOUT_SECONDS);
    }

    public static void deleteMessages(String queueUrl, List<Message> messages) {
        deleteMessagesFromList(queueUrl, messages);
    }

    public static void deleteMessages(String queueUrl, int count) {
        deleteMatchingMessages(queueUrl, count, null);
    }

    public static void deleteMatchingMessages(
            String queueUrl, int count, Map<String, String> filters) {
        deleteMatchingMessages(queueUrl, count, filters, false);
    }

    public static void deleteMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        final List<Message> messages =
                receiveMessages(queueUrl, count, filters, deleteNonMatching, true, TIMEOUT_SECONDS);
        deleteMessages(queueUrl, messages);
    }

    private static List<Message> receiveMessages(
            String queueUrl,
            int count,
            Map<String, String> filters,
            boolean deleteNonMatching,
            boolean hideReceivedMessages,
            int timeoutSeconds) {
        final boolean filterMessages = filters != null;
        final List<Message> allMessages = new ArrayList<>();
        final List<Message> targetMessages = filterMessages ? new ArrayList<>() : allMessages;

        final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest(queueUrl)
                        .withMaxNumberOfMessages(10)
                        .withWaitTimeSeconds(SQS_WAIT_TIME_SECONDS)
                        .withVisibilityTimeout(SQS_MESSAGE_VISIBILITY_TIMEOUT);

        final long startTime = System.currentTimeMillis();

        while (targetMessages.size() < count) {
            final List<Message> newMessages =
                    SQS_CLIENT.receiveMessage(receiveMessageRequest).getMessages();

            allMessages.addAll(newMessages);

            if (filterMessages) {
                newMessages.stream()
                        .filter(message -> messageMatches(message, filters))
                        .forEach(targetMessages::add);
            }

            if (System.currentTimeMillis() - startTime >= timeoutSeconds * 1000L) {
                throw new RuntimeException(
                        String.format(
                                "Received %d/%d messages after %d seconds",
                                targetMessages.size(), count, timeoutSeconds));
            }
        }

        if (filterMessages && deleteNonMatching) {
            deleteMessages(queueUrl, exclude(allMessages, targetMessages));
            resetMessageVisibility(queueUrl, targetMessages);
        } else if (!hideReceivedMessages) {
            resetMessageVisibility(queueUrl, allMessages);
        }

        return targetMessages;
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

    private static void resetMessageVisibility(String queueUrl, List<Message> messages) {
        split(messages, 10)
                .forEach(
                        batch ->
                                SQS_CLIENT.changeMessageVisibilityBatch(
                                        queueUrl,
                                        batch.stream()
                                                .map(
                                                        message ->
                                                                new ChangeMessageVisibilityBatchRequestEntry(
                                                                        message.getMessageId(),
                                                                        message.getReceiptHandle()))
                                                .collect(Collectors.toList())));
    }

    private static void deleteMessagesFromList(String queueUrl, List<Message> messages) {
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

    // --- Utils ---//

    public static <T> List<List<T>> split(List<T> list, int batchSize) {
        if (list == null) {
            throw new IllegalArgumentException("List cannot be null");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }

        List<List<T>> batches = new ArrayList<>();

        for (int idx = 0; idx < list.size(); idx += batchSize) {
            batches.add(list.subList(idx, Math.min(list.size(), idx + batchSize)));
        }

        return batches;
    }

    public static <T> List<T> exclude(List<T> source, List<T> exclude) {
        return source.stream()
                .filter(((Predicate<T>) exclude::contains).negate())
                .collect(Collectors.toList());
    }
}
