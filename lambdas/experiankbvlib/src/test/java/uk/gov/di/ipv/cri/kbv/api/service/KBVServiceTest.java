package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KBVServiceTest {
    private KBVService kbvService;
    private KBVGateway mockKbvGateway = mock(KBVGateway.class);

    @BeforeEach
    void setUp() {
        this.kbvService = new KBVService(mockKbvGateway);
    }

    @Test
    void shouldReturnAResultWhenKbvServiceIsInvokedSuccessfully() throws InterruptedException {
        QuestionsResponse answerResponseResult = mock(QuestionsResponse.class);
        QuestionAnswerRequest mockQuestionAnswerRequest = mock(QuestionAnswerRequest.class);
        when(mockKbvGateway.submitAnswers(mockQuestionAnswerRequest))
                .thenReturn(answerResponseResult);

        QuestionsResponse result = kbvService.submitAnswers(mockQuestionAnswerRequest);
        verify(mockKbvGateway).submitAnswers(mockQuestionAnswerRequest);
        assertEquals(answerResponseResult, result);
    }

    @Test
    void shouldReturnNullWhenAnExceptionOccursWhenInvokingKbvService() throws InterruptedException {
        QuestionAnswerRequest mockQuestionAnswerRequest = mock(QuestionAnswerRequest.class);

        InterruptedException expectedException = new InterruptedException();

        when(mockKbvGateway.submitAnswers(mockQuestionAnswerRequest)).thenThrow(expectedException);

        var exception =
                assertThrows(
                        InterruptedException.class,
                        () -> kbvService.submitAnswers(mockQuestionAnswerRequest));

        assertEquals(expectedException, exception);
    }
}
