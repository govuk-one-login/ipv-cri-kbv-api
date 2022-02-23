package uk.gov.di.ipv.cri.kbv.api.handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.Control;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;
import uk.gov.di.ipv.cri.kbv.api.service.ExperianService;
import uk.gov.di.ipv.cri.kbv.api.service.StorageService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.QuestionAnswerHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
public class QuestionAnswerHandlerTest {

    private QuestionAnswerHandler questionAnswerHandler;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private StorageService mockStorageService;
    @Mock private ExperianService mockExperianService;
    @Mock private APIGatewayProxyResponseEvent mockApiGatewayProxyResponseEvent;
    @Mock private Appender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {

        AWSXRay.setGlobalRecorder(
                AWSXRayRecorderBuilder.standard()
                        .withContextMissingStrategy(new LogErrorContextMissingStrategy())
                        .build());
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionAnswerHandler.class);
        logger.addAppender(appender);

        questionAnswerHandler =
                new QuestionAnswerHandler(
                        mockObjectMapper,
                        mockStorageService,
                        mockExperianService,
                        mockApiGatewayProxyResponseEvent);
    }

//    @Test
    void shouldReturn200OkWithNextQuestionWhen1stAnswerIsSubmitted()
            throws JsonProcessingException {
        APIGatewayProxyRequestEvent input = mock(APIGatewayProxyRequestEvent.class);
        Context contextMock = mock(Context.class);
        KBVSessionItem kbvSessionItemMock = mock(KBVSessionItem.class);
        QuestionState questionStateMock = mock (QuestionState.class);
        Control controlMock = mock(Control.class);
        when(controlMock.getAuthRefNo()).thenReturn("some-auth-ref");
        when(controlMock.getURN()).thenReturn("some-urn");

        when(questionStateMock.getControl()).thenReturn(controlMock);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, "new-session-id");

        QuestionAnswer questionAnswerMock = mock(QuestionAnswer.class);
        String questionID = "Q0008";
        String answer = "some-answer";

        String requestPayload = "\"questionID\":\" " +  questionID + " \",\"answer\":\" " + answer + " \"";
        when(questionAnswerMock.getQuestionId()).thenReturn(questionID);
        when(questionAnswerMock.getAnswer()).thenReturn(answer);

        when(input.getHeaders()).thenReturn(sessionHeader);
        when(mockStorageService.getSessionId(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(Optional.ofNullable(kbvSessionItemMock));

        when(mockObjectMapper.readValue(kbvSessionItemMock.getQuestionState(), QuestionState.class))
                .thenReturn(questionStateMock);

        when(input.getBody()).thenReturn(requestPayload);
        when(mockObjectMapper.readValue(requestPayload, QuestionAnswer.class)).thenReturn(questionAnswerMock);

        Question questionMock1 = mock( Question.class);
        when(questionMock1.getQuestionID()).thenReturn(questionID);

        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getQuestion()).thenReturn(questionMock1);

        Question questionMock2 = mock( Question.class);
        when(questionMock2.getQuestionID()).thenReturn("some-question-id");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock2.getQuestion()).thenReturn(questionMock2);

        when(questionStateMock.getQaPairs()).thenReturn(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        when(questionAnswerPairMock1.getAnswer()).thenReturn(answer);

        when(mockObjectMapper.writeValueAsString(questionStateMock)).thenReturn("question-state");
        doNothing().when(mockStorageService).update(kbvSessionItemMock);

        when(questionStateMock.getNextQuestion()).thenReturn(Optional.of(questionMock2));
        when(mockObjectMapper.writeValueAsString(questionMock2)).thenReturn("response-body");


        mockApiGatewayProxyResponseEvent = questionAnswerHandler.handleRequest(input, contextMock);

        assertEquals(HttpStatus.SC_OK, mockApiGatewayProxyResponseEvent.getStatusCode());
    }
}
