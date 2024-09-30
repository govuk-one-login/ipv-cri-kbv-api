package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createAuditEventExtensions;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getExperianQuestionResponse;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getKbvQuestionAnswerSummary;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionResponseWithResults;

class KbvResponsesAuditExtensionTest {
    @Test
    void shouldReturnEmptyContextEntriesWhenQuestionResponseIsEmpty() {
        QuestionsResponse questionResponse = new QuestionsResponse();

        assertEquals(Collections.emptyMap(), createAuditEventExtensions(questionResponse, false));
    }

    @Test
    void shouldReturnEmptyContextEntriesWhenQuestionResponseIsNull() {
        assertEquals(
                Collections.emptyMap(),
                createAuditEventExtensions((QuestionsResponse) null, false));
    }

    @Test
    void shouldReturnEmptyContextEntriesWhenQuestionResponseResultsIsNull() {
        QuestionsResponse questionResponse = new QuestionsResponse();
        questionResponse.setResults(null);
        assertEquals(Collections.emptyMap(), createAuditEventExtensions(questionResponse, false));
    }

    @Test
    void shouldReturnEmptyWhenQuestionResponseStatusIsNullAndHasNoAlerts() {
        QuestionsResponse questionResponse = new QuestionsResponse();
        KbvResult kbvResult = new KbvResult();
        questionResponse.setResults(kbvResult);
        assertEquals(Collections.emptyMap(), createAuditEventExtensions(questionResponse, false));
    }

    @Test
    void shouldReturnEmptyContextEntriesWhenKbvItemIsEmpty() {
        KBVItem kbvItem = new KBVItem();

        assertEquals(
                Collections.emptyMap(),
                createAuditEventExtensions(
                        kbvItem.getStatus(), kbvItem.getQuestionAnswerResultSummary()));
    }

    @ParameterizedTest(
            name =
                    "{index} => authenticationResult={0}, answeredCorrectly={1}, answeredInCorrectly={2}, totalQuestionsAsked={3}")
    @CsvSource({
        "Authenticated, 3, 1, 4",
        "Not Authenticated, 2, 2, 4",
    })
    void shouldReturnContextEntriesWithCorrectExtensionFieldsFromQuestionResponse(
            String authenticationResult,
            int answeredCorrectly,
            int answeredInCorrectly,
            int totalQuestionsAsked) {

        QuestionsResponse questionResponses =
                getQuestionResponseWithResults(
                        authenticationResult,
                        getKbvQuestionAnswerSummary(
                                answeredCorrectly, answeredInCorrectly, totalQuestionsAsked));

        assertEquals(
                TestDataCreator.createAuditExtensionsMap(
                        authenticationResult,
                        answeredCorrectly,
                        answeredInCorrectly,
                        totalQuestionsAsked),
                createAuditEventExtensions(questionResponses, false));
    }

    @ParameterizedTest(
            name =
                    "{index} => authenticationResult={0}, answeredCorrectly={1}, answeredInCorrectly={2}, totalQuestionsAsked={3}")
    @CsvSource({"Authenticated, 3, 1, 4", "Not Authenticated, 2, 2, 4"})
    void shouldReturnContextEntriesWithSummarizedKbvResultsFromKbvItem(
            String authenticationResult,
            int answeredCorrectly,
            int answeredInCorrectly,
            int totalQuestionsAsked) {

        KBVItem kbvItem = new KBVItem();
        kbvItem.setStatus(authenticationResult);
        kbvItem.setQuestionAnswerResultSummary(
                getKbvQuestionAnswerSummary(
                        answeredCorrectly, answeredInCorrectly, totalQuestionsAsked));

        assertEquals(
                TestDataCreator.createAuditExtensionsMap(
                        authenticationResult,
                        answeredCorrectly,
                        answeredInCorrectly,
                        totalQuestionsAsked),
                createAuditEventExtensions(
                        kbvItem.getStatus(), kbvItem.getQuestionAnswerResultSummary()));
    }

    @Test
    void shouldReturnContextEntriesWithKbvResultsOutcomeWhenNoResultSummary() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setStatus("Not Authenticated");

        assertEquals(
                Map.of("outcome", "Not Authenticated"),
                createAuditEventExtensions(
                        kbvItem.getStatus(), kbvItem.getQuestionAnswerResultSummary()));
    }

    @Test
    void shouldCreateEmptyAuditExtensionsFromSAAQuestionResponse() {
        assertEquals(
                Collections.emptyMap(),
                createAuditEventExtensions(getExperianQuestionResponse(false), true));
    }

    @Test
    void shouldCreateAuditExtensionsWithAlertFromSAAQuestionResponseWithAlert() {
        assertEquals(
                Map.of("repeatAttemptAlert", true),
                createAuditEventExtensions(getExperianQuestionResponse(true), true));
    }
}
