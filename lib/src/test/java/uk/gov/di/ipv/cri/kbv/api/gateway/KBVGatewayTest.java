package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandler;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator;

import javax.xml.ws.soap.SOAPFaultException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KBVGatewayTest {
    @Mock private StartAuthnAttemptRequestMapper mockSAARequestMapper;
    @Mock private ResponseToQuestionMapper mockResponseToQuestionMapper;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;
    @InjectMocks private KBVGateway kbvGateway;

    @Test
    void shouldCallGetQuestionsSuccessfully() {
        QuestionRequest questionRequest = mock(QuestionRequest.class);
        ArgumentCaptor<SAAResponse2> saaResponse2Captor =
                ArgumentCaptor.forClass(SAAResponse2.class);

        SAARequest mockSaaRequest = mock(SAARequest.class);
        when(mockSAARequestMapper.mapQuestionRequest(questionRequest)).thenReturn(mockSaaRequest);

        kbvGateway.getQuestions(questionRequest);

        verify(mockSAARequestMapper).mapQuestionRequest(questionRequest);
        verify(mockSAARequestMapper)
                .mapSAAResponse2ToQuestionsResponse(saaResponse2Captor.capture());
    }

    @Test
    void shouldCallSubmitAnswersSuccessfully() {
        QuestionAnswerRequest questionAnswerRequest =
                TestDataCreator.createTestQuestionAnswerRequest();

        ArgumentCaptor<RTQResponse2> mockRtqResponse = ArgumentCaptor.forClass(RTQResponse2.class);
        RTQRequest mockRtqRequest = new RTQRequest();

        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);

        kbvGateway.submitAnswers(questionAnswerRequest);

        verify(mockResponseToQuestionMapper).mapQuestionAnswersRtqRequest(questionAnswerRequest);
        verify(mockResponseToQuestionMapper)
                .mapRTQResponse2ToMapQuestionsResponse(mockRtqResponse.capture());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdentityIQWebServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    new KBVGateway(mockSAARequestMapper, mockResponseToQuestionMapper, null);
                },
                "identityIQWebServiceSoap must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSaaRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    new KBVGateway(
                            null, mockResponseToQuestionMapper, mockIdentityIQWebServiceSoap);
                },
                "identityIQWebServiceSoap must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRtqRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    new KBVGateway(mockSAARequestMapper, null, mockIdentityIQWebServiceSoap);
                },
                "identityIQWebServiceSoap must not be null");
    }

    @Test
    void shouldThrowSOAPFaultExceptionWhenInvokingKbvServiceWithBadHeaderHandler() {
        QuestionAnswerRequest mockQuestionAnswerRequest = mock(QuestionAnswerRequest.class);

        HeaderHandler headerHandler = mock(HeaderHandler.class);

        KBVGateway kbvGateway =
                new KBVGateway(
                        mock(StartAuthnAttemptRequestMapper.class),
                        mock(ResponseToQuestionMapper.class),
                        new KBVClientFactory(
                                        new IdentityIQWebService(),
                                        new HeaderHandlerResolver(headerHandler),
                                        "endpoint")
                                .createClient());

        assertThrows(
                SOAPFaultException.class,
                () -> kbvGateway.submitAnswers(mockQuestionAnswerRequest));
    }
}
