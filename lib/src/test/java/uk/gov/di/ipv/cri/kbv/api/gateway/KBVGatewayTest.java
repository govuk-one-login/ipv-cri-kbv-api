package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KBVGatewayTest {
    @Mock private StartAuthnAttemptRequestMapper mockSAARequestMapper;
    @Mock private ResponseToQuestionMapper mockResponseToQuestionMapper;
    @Mock private QuestionsResponseMapper mockQuestionsResponseMapper;
    @Mock private IdentityIQWebServiceSoap mockIdentityIQWebServiceSoap;
    @Mock private MetricsService mockMetricsService;
    @InjectMocks private KBVGateway kbvGateway;

    @Test
    void shouldCallGetQuestionsSuccessfully() {
        QuestionRequest questionRequest = mock(QuestionRequest.class);

        SAARequest mockSaaRequest = mock(SAARequest.class);
        SAAResponse2 mockSaaResponse = mock(SAAResponse2.class);
        QuestionsResponse mockQuestionsResponse = mock(QuestionsResponse.class);
        KbvResult mockKbvResult = mock(KbvResult.class);
        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockSAARequestMapper.mapQuestionRequest(questionRequest)).thenReturn(mockSaaRequest);
        when(mockIdentityIQWebServiceSoap.saa(mockSaaRequest)).thenReturn(mockSaaResponse);
        when(mockQuestionsResponseMapper.mapSAAResponse(mockSaaResponse))
                .thenReturn(mockQuestionsResponse);

        kbvGateway.getQuestions(questionRequest);

        verify(mockSAARequestMapper).mapQuestionRequest(questionRequest);
        verify(mockIdentityIQWebServiceSoap).saa(mockSaaRequest);
        verify(mockQuestionsResponseMapper).mapSAAResponse(mockSaaResponse);
        verify(mockMetricsService).sendResultMetric(mockKbvResult, "initial_questions_response");
    }

    @Test
    void shouldCallSubmitAnswersSuccessfully() {
        QuestionAnswerRequest questionAnswerRequest = mock(QuestionAnswerRequest.class);
        RTQRequest mockRtqRequest = mock(RTQRequest.class);
        RTQResponse2 mockRtqResponse = mock(RTQResponse2.class);
        QuestionsResponse mockQuestionsResponse = mock(QuestionsResponse.class);
        KbvResult mockKbvResult = mock(KbvResult.class);
        when(mockQuestionsResponse.getResults()).thenReturn(mockKbvResult);
        when(mockResponseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest))
                .thenReturn(mockRtqRequest);
        when(mockIdentityIQWebServiceSoap.rtq(mockRtqRequest)).thenReturn(mockRtqResponse);
        when(mockQuestionsResponseMapper.mapRTQResponse(mockRtqResponse))
                .thenReturn(mockQuestionsResponse);

        kbvGateway.submitAnswers(questionAnswerRequest);

        verify(mockResponseToQuestionMapper).mapQuestionAnswersRtqRequest(questionAnswerRequest);
        verify(mockIdentityIQWebServiceSoap).rtq(mockRtqRequest);
        verify(mockQuestionsResponseMapper).mapRTQResponse(mockRtqResponse);
        verify(mockMetricsService).sendResultMetric(mockKbvResult, "submit_questions_response");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdentityIQWebServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                null,
                                mockMetricsService),
                "identityIQWebServiceSoap must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSaaRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                null,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                mockIdentityIQWebServiceSoap,
                                mockMetricsService),
                "saaRequestMapper must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenQuestionsResponseMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                null,
                                mockIdentityIQWebServiceSoap,
                                mockMetricsService),
                "questionsResponseMapper must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMetricsServiceIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                mockResponseToQuestionMapper,
                                mockQuestionsResponseMapper,
                                mockIdentityIQWebServiceSoap,
                                null),
                "metricsService must not be null");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRtqRequestMapperIsNull() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new KBVGateway(
                                mockSAARequestMapper,
                                null,
                                mockQuestionsResponseMapper,
                                mockIdentityIQWebServiceSoap,
                                mockMetricsService),
                "rtqRequestMapper must not be null");
    }
}
