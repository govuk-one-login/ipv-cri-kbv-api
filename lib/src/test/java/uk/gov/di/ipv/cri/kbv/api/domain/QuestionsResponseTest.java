package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionsResponseTest {

    @Test
    void shouldReturnResponsesWithQuestions() {
        QuestionsResponse response = new QuestionsResponse();
        assertFalse(response.hasQuestions());

        KbvQuestion[] questions = {new KbvQuestion(), new KbvQuestion()};
        response.setQuestions(questions);
        assertTrue(response.hasQuestions());
    }

    @Nested
    class ResponseEnded {
        @Test
        void shouldReturnTrueWhenQuestionResponseEnded() {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.hasQuestionRequestEnded());

            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.hasQuestionRequestEnded());

            result.setNextTransId(new String[] {"END"});
            assertTrue(response.hasQuestionRequestEnded());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "RTQ", "SAA"})
        void shouldReturnFalseWhenQuestionResponseIsRTQ(String status) {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.hasQuestionRequestEnded());

            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.hasQuestionRequestEnded());

            result.setNextTransId(new String[] {status});
            assertFalse(response.hasQuestionRequestEnded());
        }
    }

    @Nested
    class ThinFile {
        @Test
        void shouldReturnTrueWnenResponseHasEndedAndStatusIsUnableToAuthenticate() {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.isThinFile());

            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.isThinFile());

            result.setNextTransId(new String[] {"END"});
            result.setAuthenticationResult("Unable to Authenticate");

            assertTrue(response.isThinFile());
        }

        @Test
        void shouldReturnFalseWnenResponseIsRTQAndStatusIsAuthenticated() {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.isThinFile());

            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.isThinFile());

            result.setNextTransId(new String[] {"RTQ"});
            result.setAuthenticationResult("Authenticated");

            assertFalse(response.isThinFile());
        }

        @Test
        void shouldReturnFalseWnenResponseHasEndedAndStatusIsUnknown() {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.isThinFile());

            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.isThinFile());

            result.setNextTransId(new String[] {"END"});
            result.setAuthenticationResult(null);

            assertFalse(response.isThinFile());
        }
    }
}
