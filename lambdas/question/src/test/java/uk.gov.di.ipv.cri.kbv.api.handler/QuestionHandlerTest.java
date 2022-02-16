package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.*;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuestionHandlerTest {

    private static final String TEST_IPV_SESSION_ID = "a-session-id";
    private QuestionHandler questionHandler;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private StorageService mockStorageService;
    @Mock
    private ExperianService mockExperianService;
    @Mock
    private APIGatewayProxyResponseEvent mockapiGatewayProxyResponseEvent;


    private final String expectedResult = "{\"questionID\":\"Q00039\",\"text\":\"What is the balance, including interest, of your  loan?\",\"tooltip\":\"The approximate amount in £s on a current active personal loan. Does not include HP Loans, 2nd Mortgages or Home Credit.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"UP TO £6,250\",\"OVER £6,250 UP TO £6,500\",\"OVER £6,500 UP TO £6,750\",\"OVER £6,750 UP TO £7,000\",\"NONE OF THE ABOVE / DOES NOT APPLY\"]}}";


    @BeforeEach
    void setUp() {
        AWSXRay.beginSegment("handleRequest");
        questionHandler =
                new QuestionHandler(mockObjectMapper, mockStorageService, mockExperianService);
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

    @Test
    void shouldReturn200ResponseWithAQuestion() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setHeaders(Map.of(QuestionHandler.HEADER_SESSION_ID, TEST_IPV_SESSION_ID));

        PersonIdentity personIdentity = mock(PersonIdentity.class);
        QuestionState expectedQuestionState = objectMapper.readValue(TestData.EXPECTED_QUESTION_STATE, QuestionState.class);

        KBVSessionItem kbvSessionItem = mock(KBVSessionItem.class);
        when(mockStorageService.getSessionId(TEST_IPV_SESSION_ID)).thenReturn(kbvSessionItem);

        when(mockObjectMapper.readValue(kbvSessionItem.getUserAttributes(), PersonIdentity.class))
                .thenReturn(personIdentity);
        when(mockObjectMapper.readValue(kbvSessionItem.getQuestionState(), QuestionState.class))
                .thenReturn(expectedQuestionState);

        when(mockObjectMapper.writeValueAsString(personIdentity))
                .thenReturn("person-identity");

        Question question = objectMapper.readValue(expectedResult, Question.class);
        QuestionState mockQuestionState = mock(QuestionState.class);
        when(mockQuestionState.getNextQuestion()).thenReturn(Optional.empty());


        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        when(mockExperianService.getQuestions("person-identity")).thenReturn(questionsResponse);
        when(mockQuestionState.setQuestionsResponse(questionsResponse)).thenReturn(true);

        when(mockapiGatewayProxyResponseEvent.getBody()).thenReturn(expectedResult);

         mockapiGatewayProxyResponseEvent = questionHandler.handleRequest(input, mock(Context.class));

//        verify(mockApiGatewayProxyResponseEvent).withHeaders(Map.of("Content-Type",
//                "application/json"));
//        verify(mockApiGatewayProxyResponseEvent).withStatusCode(HttpStatus.SC_OK);
//        assertTrue(apiGatewayProxyResponseEvent.getBody().contains("Q007"));
        assertEquals( HttpStatus.SC_OK, mockapiGatewayProxyResponseEvent.getStatusCode());
    }
}
