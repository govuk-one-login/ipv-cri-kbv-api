package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KBVServiceTest {
    @Mock private KBVGateway mockKbvGateway;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;

    private KBVService kbvService;

    @BeforeEach
    void setUp() {
        this.kbvService = new KBVService(mockKbvGateway);
    }

    @Test
    void shouldReturnAResultWhenKbvServiceIsInvokedSuccessfully() {
        QuestionsResponse answerResponseResult = mock(QuestionsResponse.class);
        QuestionAnswerRequest mockQuestionAnswerRequest = mock(QuestionAnswerRequest.class);
        when(mockKbvGateway.submitAnswers(mockIdentityIQWebServiceSoap, mockQuestionAnswerRequest))
                .thenReturn(answerResponseResult);

        QuestionsResponse result =
                kbvService.submitAnswers(mockIdentityIQWebServiceSoap, mockQuestionAnswerRequest);
        verify(mockKbvGateway)
                .submitAnswers(mockIdentityIQWebServiceSoap, mockQuestionAnswerRequest);
        assertEquals(answerResponseResult, result);
    }
}
