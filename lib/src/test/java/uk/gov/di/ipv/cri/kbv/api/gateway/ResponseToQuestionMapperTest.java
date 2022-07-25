package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ResponseToQuestionMapperTest {
    private ResponseToQuestionMapper responseToQuestionMapper;
    private QuestionAnswerRequest questionAnswerRequest;

    @BeforeEach
    void setup() {
        responseToQuestionMapper = new ResponseToQuestionMapper();
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
        assertEquals(0, result.getResponses().getResponse().get(0).getCustResponseFlag());
        assertEquals("U", result.getResponses().getResponse().get(0).getAnswerActionFlag());
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
