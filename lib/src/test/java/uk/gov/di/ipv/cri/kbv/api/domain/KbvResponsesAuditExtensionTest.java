package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createAuditEventExtensions;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getKbvQuestionAnswerSummary;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionResponseWithResults;

class KbvResponsesAuditExtensionTest {
    @Test
    void shouldReturnEmptyContextEntriesWhenQuestionResponseIsEmpty() {
        QuestionsResponse questionResponse = new QuestionsResponse();

        assertEquals(Collections.emptyMap(), createAuditEventExtensions(questionResponse));
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
    @CsvSource({"Authenticated, 3, 1, 4", "Not Authenticated, 2, 2, 4"})
    void shouldReturnContextEntriesWithSummarizedKbvResponseFromQuestionResponse(
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
                Map.of(
                        "totalQuestionsAnsweredCorrect", answeredCorrectly,
                        "totalQuestionsAsked", totalQuestionsAsked,
                        "totalQuestionsAnsweredIncorrect", answeredInCorrectly,
                        "outcome", authenticationResult),
                createAuditEventExtensions(questionResponses));
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
                Map.of(
                        "totalQuestionsAnsweredCorrect", answeredCorrectly,
                        "totalQuestionsAsked", totalQuestionsAsked,
                        "totalQuestionsAnsweredIncorrect", answeredInCorrectly,
                        "outcome", authenticationResult),
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
}
