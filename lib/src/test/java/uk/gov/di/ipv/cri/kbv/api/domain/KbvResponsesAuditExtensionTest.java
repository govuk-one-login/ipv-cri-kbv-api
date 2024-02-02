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

    @ParameterizedTest(
            name =
                    "{index} => authenticationResult={0}, answeredCorrectly={1}, answeredInCorrectly={2}, totalQuestionsAsked={3}")
    @CsvSource({"Authenticated, 3, 1, 4", "Not Authenticated, 2, 2, 4"})
    void shouldReturnContextEntriesWithSummarizedKbvQuestionResponses(
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
}
