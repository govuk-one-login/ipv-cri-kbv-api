package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;
import uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseToQuestionMapperTest {
    private ResponseToQuestionMapper responseToQuestionMapper;
    private QuestionAnswerRequest questionAnswerRequest;

    @Mock private MetricsService metricsService;

    @BeforeEach
    void setup() {
        responseToQuestionMapper = new ResponseToQuestionMapper(metricsService);
    }

    @Test
    void shouldSendMetrics() {
        RTQResponse2 response = mock(RTQResponse2.class);
        Results results = mock(Results.class);
        when(response.getResults()).thenReturn(results);
        Error error = mock(Error.class);
        when(response.getError()).thenReturn(error);
        responseToQuestionMapper.mapRTQResponse2ToMapQuestionsResponse(response);
        verify(metricsService).sendResultMetric(results, "submit_questions_response");
        verify(metricsService).sendErrorMetric(error, "submit_questions_response_error");
    }

    @Test
    void shouldConvertAnAnswerQuestionRequestInToARtqRequest() {
        questionAnswerRequest = TestDataCreator.createTestQuestionAnswerRequest();

        RTQRequest result =
                responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);

        assertNotNull(result);

        assertEquals(questionAnswerRequest.getUrn(), result.getControl().getURN());
        assertEquals(questionAnswerRequest.getAuthRefNo(), result.getControl().getAuthRefNo());
        assertEquals(
                questionAnswerRequest.getQuestionAnswers().get(0).getQuestionId(),
                result.getResponses().getResponse().get(0).getQuestionID());
        assertEquals(
                questionAnswerRequest.getQuestionAnswers().get(0).getAnswer(),
                result.getResponses().getResponse().get(0).getAnswerGiven());
    }

    @Test
    void shouldThrowExceptionWhenQuestionAnswerRequestIsNull() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                responseToQuestionMapper.mapQuestionAnswersRtqRequest(
                                        questionAnswerRequest));
        assertEquals("The QuestionAnswerRequest must not be null", exception.getMessage());
    }
}
