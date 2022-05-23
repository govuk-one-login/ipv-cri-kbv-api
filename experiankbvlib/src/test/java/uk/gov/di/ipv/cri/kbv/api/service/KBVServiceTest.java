package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.ResponseToQuestionMapper;
import uk.gov.di.ipv.cri.kbv.api.gateway.StartAuthnAttemptRequestMapper;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

import javax.xml.ws.soap.SOAPFaultException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldThrowSOAPFaultExceptionWhenInvokingKbvServiceWithBadHeaderHandler() {
        QuestionAnswerRequest mockQuestionAnswerRequest = mock(QuestionAnswerRequest.class);

        HeaderHandler headerHandler = mock(HeaderHandler.class);
        when(headerHandler.handleMessage(any())).thenThrow(RuntimeException.class);

        KBVGateway kbvGateway =
                new KBVGateway(
                        mock(StartAuthnAttemptRequestMapper.class),
                        mock(ResponseToQuestionMapper.class),
                        new KBVClientFactory(
                                        new IdentityIQWebService(),
                                        new HeaderHandlerResolver(headerHandler))
                                .createClient());

        kbvService = new KBVService(kbvGateway);

        assertThrows(
                SOAPFaultException.class,
                () -> kbvService.submitAnswers(mockQuestionAnswerRequest));
        verify(headerHandler).handleMessage(any());
    }
}
