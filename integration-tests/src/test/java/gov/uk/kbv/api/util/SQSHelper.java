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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class to read and delete messages from SQS queues in a predictable manner. The class is
 * a wrapper around {@link AmazonSQS} and allows to get around the message retrieval limitations of
 * the SQS service.
 */
public final class SQSHelper {

    /** The default total timeout for queue operations */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /** Helper class to capture received messages */
    private static class FilteredMessages {
        public FilteredMessages(List<Message> matchingMessages, List<Message> nonMatchingMessages) {
            this.matchingMessages = Objects.requireNonNull(matchingMessages);
            this.nonMatchingMessages = nonMatchingMessages;
        }

        public List<Message> allMessages() {
            return Stream.of(matchingMessages, nonMatchingMessages)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        public List<Message> matchingMessages;
        public List<Message> nonMatchingMessages;
    }

    private final AmazonSQS sqsClient;
    private final ObjectMapper objectMapper;

    /**
     * The total timeout for queue operations. Defaults to {@link SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     */
    private int timeoutSeconds;

    private int sqsWaitTimeSeconds;
    private int sqsMessageVisibilityTimeout;

    /**
     * Use the {@link SQSHelper#DEFAULT_TIMEOUT_SECONDS default timeout}
     *
     * @see SQSHelper#SQSHelper(int, AmazonSQS, ObjectMapper)
     */
    public SQSHelper() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Specify a total timeout for queue operations
     *
     * @see SQSHelper#SQSHelper(int, AmazonSQS, ObjectMapper)
     */
    public SQSHelper(int timeoutSeconds) {
        this(timeoutSeconds, null, null);
    }

    /**
     * Optionally provide a custom SQS client and object mapper. The values can be {@code null} to
     * use the default constructors. The default timeout value can be accessed through {@link
     * SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     */
    public SQSHelper(int timeoutSeconds, AmazonSQS sqsClient, ObjectMapper objectMapper) {
        this.setTimeout(timeoutSeconds);
        this.sqsClient = sqsClient == null ? AmazonSQSClientBuilder.defaultClient() : sqsClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * Set the total timeout for queue operations. Secondary timeouts are calculated based on the
     * provided value. The default timeout can be accessed through {@link
     * SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     */
    public void setTimeout(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.sqsWaitTimeSeconds = Math.min(20, timeoutSeconds);
        this.sqsMessageVisibilityTimeout = this.timeoutSeconds + this.sqsWaitTimeSeconds + 5;
    }

    /**
     * Receive at least {@code count} messages from the queue. Messages are not hidden from
     * subsequent requests. The method throws if at least {@code count} messages have not been found
     * within the {@link SQSHelper#timeoutSeconds timeout}.
     *
     * @param queueUrl The URL of the SQS queue
     * @param count The desired number of messages to receive. Exactly {@code count} or more
     *     messages are returned
     * @return The list of at least {@code count} received messages, or an empty list if {@code
     *     count} is less than 1
     * @see AmazonSQS#changeMessageVisibilityBatch(String, List)
     */
    public List<Message> receiveMessages(String queueUrl, int count) {
        return receiveMessages(queueUrl, count, null, false);
    }

    /**
     * Select messages from the queue based on the provided JSON body {@code filters} and receive at
     * least {@code count} matching messages.
     *
     * @param count The desired number of messages matching the provided {@code filters}. Exactly
     *     {@code count} or more messages are returned
     * @param filters Key-value pairs to filter and only select messages whose JSON body contents
     *     match all the provided pairs. Messages without a valid JSON body are ignored.
     * @return The list of at least {@code count} received messages matching the {@code filters}
     * @see SQSHelper#receiveMessages(String, int)
     */
    public List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters) {
        return receiveMatchingMessages(queueUrl, count, filters, false);
    }

    /**
     * Select messages from the queue and optionally delete any other encountered messages not
     * matching the specified criteria.
     *
     * @param deleteNonMatching Set to {@code true} to delete any messages that were received from
     *     the queue but did not match the provided {@link SQSHelper#receiveMatchingMessages(String,
     *     int, Map) filters}. This does not clear the queue but deletes any additional messages
     *     received from SQS.
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        return receiveMessages(queueUrl, count, Objects.requireNonNull(filters), deleteNonMatching);
    }

    /**
     * Delete all messages from the provided list
     *
     * @see AmazonSQS#deleteMessageBatch(String, List)
     */
    public void deleteMessages(String queueUrl, List<Message> messages) {
        deleteMessagesFromList(queueUrl, messages);
    }

    /**
     * Delete a number of messages from the queue within the {@link SQSHelper#timeoutSeconds
     * timeout}. Messages are deleted in the order they're received from SQS. Exactly {@code count}
     * or more messages are deleted.
     *
     * @see SQSHelper#receiveMessages(String, int)
     */
    public void deleteMessages(String queueUrl, int count) {
        deleteMessages(queueUrl, count, null, false);
    }

    /**
     * Delete at least {@code count} messages from the queue based on the provided JSON body {@code
     * filters}.
     *
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public void deleteMatchingMessages(String queueUrl, int count, Map<String, String> filters) {
        deleteMatchingMessages(queueUrl, count, filters, false);
    }

    /**
     * Delete at least {@code count} messages from the queue and optionally delete any other
     * encountered messages not matching the specified criteria.
     *
     * @see SQSHelper#deleteMatchingMessages(String, int, Map)
     * @see SQSHelper#receiveMatchingMessages(String, int, Map, boolean)
     */
    public void deleteMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        deleteMessages(queueUrl, count, Objects.requireNonNull(filters), deleteNonMatching);
    }

    private List<Message> receiveMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        final FilteredMessages messages = getMessages(queueUrl, count, filters);

        if (deleteNonMatching) {
            deleteMessagesFromList(queueUrl, messages.nonMatchingMessages);
            resetMessageVisibility(queueUrl, messages.matchingMessages);
        } else {
            resetMessageVisibility(queueUrl, messages.allMessages());
        }

        return messages.matchingMessages;
    }

    private void deleteMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching) {
        final FilteredMessages messages = getMessages(queueUrl, count, filters);

        if (deleteNonMatching) {
            deleteMessagesFromList(queueUrl, messages.allMessages());
            return;
        }

        deleteMessagesFromList(queueUrl, messages.matchingMessages);

        if (filters != null) {
            resetMessageVisibility(queueUrl, messages.nonMatchingMessages);
        }
    }

    private FilteredMessages getMessages(String queueUrl, int count, Map<String, String> filters) {
        final boolean filterMessages = filters != null;
        final List<Message> allMessages = new ArrayList<>();
        final List<Message> targetMessages = filterMessages ? new ArrayList<>() : allMessages;

        final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest(queueUrl)
                        .withMaxNumberOfMessages(10)
                        .withWaitTimeSeconds(this.sqsWaitTimeSeconds)
                        .withVisibilityTimeout(this.sqsMessageVisibilityTimeout);

        final long startTime = System.currentTimeMillis();

        while (targetMessages.size() < count) {
            final List<Message> newMessages =
                    this.sqsClient.receiveMessage(receiveMessageRequest).getMessages();

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

        return new FilteredMessages(
                targetMessages, filterMessages ? exclude(allMessages, targetMessages) : null);
    }

    private void deleteMessagesFromList(String queueUrl, List<Message> messages) {
        if (Objects.requireNonNull(messages).isEmpty()) {
            return;
        }

        this.sqsClient.deleteMessageBatch(
                queueUrl,
                messages.stream()
                        .map(SQSHelper::deleteMessageRequest)
                        .collect(Collectors.toList()));
    }

    private void resetMessageVisibility(String queueUrl, List<Message> messages) {
        if (Objects.requireNonNull(messages).isEmpty()) {
            return;
        }

        split(messages, 10)
                .forEach(
                        batch ->
                                this.sqsClient.changeMessageVisibilityBatch(
                                        queueUrl,
                                        batch.stream()
                                                .map(SQSHelper::changeMessageVisibilityRequest)
                                                .collect(Collectors.toList())));
    }

    private boolean messageMatches(Message message, Map<String, String> properties) {
        final JsonNode body = parseJson(message.getBody());
        return body != null
                && properties.keySet().stream()
                        .allMatch(key -> body.at(key).asText().equals(properties.get(key)));
    }

    private JsonNode parseJson(String json) {
        try {
            return this.objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static DeleteMessageBatchRequestEntry deleteMessageRequest(Message message) {
        return new DeleteMessageBatchRequestEntry(
                message.getMessageId(), message.getReceiptHandle());
    }

    private static ChangeMessageVisibilityBatchRequestEntry changeMessageVisibilityRequest(
            Message message) {
        return new ChangeMessageVisibilityBatchRequestEntry(
                message.getMessageId(), message.getReceiptHandle());
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
