package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

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

    @Nested
    class RepeatAttemptAlert {
        @Test
        void shouldReturnTrueWhenResponseHasRepeatAttemptAlert() {
            QuestionsResponse response = new QuestionsResponse();
            KbvResult result = new KbvResult();
            KbvAlert alert = new KbvAlert();
            alert.setCode("U501");
            alert.setText("Applicant has previously requested authentication");
            result.setAlerts(Collections.singletonList(alert));
            response.setResults(result);

            assertTrue(response.isRepeatAttemptAlert());
        }

        @Test
        void shouldReturnFalseWhenResponseAlertsEmpty() {
            QuestionsResponse response = new QuestionsResponse();
            KbvResult result = new KbvResult();
            result.setAlerts(Collections.emptyList());
            response.setResults(result);

            assertFalse(response.isRepeatAttemptAlert());
        }

        @Test
        void shouldReturnFalseWhenResponseAlertsDoesNotContainAlertWithAlertCode() {
            QuestionsResponse response = new QuestionsResponse();
            KbvResult result = new KbvResult();
            KbvAlert alert = new KbvAlert();
            alert.setCode("Not the correct code");
            alert.setText("Applicant has previously requested authentication");
            result.setAlerts(Collections.singletonList(alert));
            response.setResults(result);

            assertFalse(response.isRepeatAttemptAlert());
        }

        @Test
        void shouldReturnFalseWhenResponseAlertsIsNull() {
            QuestionsResponse response = new QuestionsResponse();
            KbvResult result = new KbvResult();
            response.setResults(result);
            assertFalse(response.isRepeatAttemptAlert());
        }

        @Test
        void shouldReturnFalseWhenResponseResultsIsNull() {
            QuestionsResponse response = new QuestionsResponse();
            assertFalse(response.isRepeatAttemptAlert());
        }
    }
}
