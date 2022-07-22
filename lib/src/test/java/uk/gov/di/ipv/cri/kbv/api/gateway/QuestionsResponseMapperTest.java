package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.AnswerFormat;
import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.ResultsQuestions;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionsResponseMapperTest {

    private static final String AUTH_REF_NO = "auth-ref";
    private static final String URN = String.valueOf(UUID.randomUUID());
    private static final String ERROR_CODE = "error-code";
    private static final String ERROR_MESSAGE = "error-message";
    private static final String QUESTION_ID = "question-id";
    private static final String QUESTION_TEXT = "question-text";
    private static final String QUESTION_TOOLTIP = "tooltip";
    private static final String FIELD_TYPE = "field-type";
    private static final String ANSWER_FORMAT_ID = "answer-format-id";
    private static final String OPTION_ONE = "answer-1";
    private static final String OPTION_TWO = "answer-2";
    private static final String OUTCOME = "outcome";
    private static final String AUTH_RESULT = "auth-result";
    private static final String CONFIRMATION_CODE = "conf-code";
    private static final String TRANS_ID = "trans-id";
    private static final int CORRECT_ANSWERS = 2;
    private static final int INCORRECT_ANSWERS = 1;
    private static final int QUESTIONS_ASKED = 3;
    private static final int QUESTIONS_SKIPPED = 0;

    private QuestionsResponseMapper questionsResponseMapper;

    @BeforeEach
    void setup() {
        questionsResponseMapper = new QuestionsResponseMapper();
    }

    @Test
    void shouldMapRTQResponse() {
        Control mockControl = createControl();
        Error mockError = createError();
        Questions mockQuestions = createQuestions();
        Results mockResults = createResults();

        RTQResponse2 rtqResponse = mock(RTQResponse2.class);
        when(rtqResponse.getControl()).thenReturn(mockControl);
        when(rtqResponse.getError()).thenReturn(mockError);
        when(rtqResponse.getQuestions()).thenReturn(mockQuestions);
        when(rtqResponse.getResults()).thenReturn(mockResults);

        QuestionsResponse questionsResponse = questionsResponseMapper.mapRTQResponse(rtqResponse);

        makeQuestionsResponseAssertions(questionsResponse);
    }

    @Test
    void shouldMapSAAResponse() {
        Control mockControl = createControl();
        Error mockError = createError();
        Questions mockQuestions = createQuestions();
        Results mockResults = createResults();

        SAAResponse2 mockSaaResponse = mock(SAAResponse2.class);
        when(mockSaaResponse.getControl()).thenReturn(mockControl);
        when(mockSaaResponse.getError()).thenReturn(mockError);
        when(mockSaaResponse.getQuestions()).thenReturn(mockQuestions);
        when(mockSaaResponse.getResults()).thenReturn(mockResults);

        QuestionsResponse questionsResponse =
                questionsResponseMapper.mapSAAResponse(mockSaaResponse);

        makeQuestionsResponseAssertions(questionsResponse);
    }

    private void makeQuestionsResponseAssertions(QuestionsResponse questionsResponse) {
        assertEquals(AUTH_REF_NO, questionsResponse.getAuthReference());
        assertEquals(URN, questionsResponse.getUniqueReference());
        assertEquals(ERROR_CODE, questionsResponse.getErrorCode());
        assertEquals(ERROR_MESSAGE, questionsResponse.getErrorMessage());
        KbvQuestion mappedQuestion = questionsResponse.getQuestions()[0];
        assertEquals(QUESTION_ID, mappedQuestion.getQuestionId());
        assertEquals(QUESTION_TEXT, mappedQuestion.getText());
        assertEquals(QUESTION_TOOLTIP, mappedQuestion.getTooltip());
        assertEquals(FIELD_TYPE, mappedQuestion.getQuestionOptions().getFieldType());
        assertEquals(ANSWER_FORMAT_ID, mappedQuestion.getQuestionOptions().getIdentifier());
        assertEquals(OPTION_ONE, mappedQuestion.getQuestionOptions().getOptions().get(0));
        assertEquals(OPTION_TWO, mappedQuestion.getQuestionOptions().getOptions().get(1));
        assertEquals(OUTCOME, questionsResponse.getResults().getOutcome());
        assertEquals(AUTH_RESULT, questionsResponse.getResults().getAuthenticationResult());
        assertEquals(CONFIRMATION_CODE, questionsResponse.getResults().getConfirmationCode());
        assertEquals(TRANS_ID, questionsResponse.getResults().getNextTransId()[0]);
        KbvQuestionAnswerSummary mappedAnswerSummary =
                questionsResponse.getResults().getAnswerSummary();
        assertEquals(CORRECT_ANSWERS, mappedAnswerSummary.getAnsweredCorrect());
        assertEquals(INCORRECT_ANSWERS, mappedAnswerSummary.getAnsweredIncorrect());
        assertEquals(QUESTIONS_ASKED, mappedAnswerSummary.getQuestionsAsked());
        assertEquals(QUESTIONS_SKIPPED, mappedAnswerSummary.getQuestionsSkipped());
    }

    private Control createControl() {
        Control mockControl = mock(Control.class);
        when(mockControl.getAuthRefNo()).thenReturn(AUTH_REF_NO);
        when(mockControl.getURN()).thenReturn(URN);
        return mockControl;
    }

    private Error createError() {
        Error mockError = mock(Error.class);
        when(mockError.getErrorCode()).thenReturn(ERROR_CODE);
        when(mockError.getMessage()).thenReturn(ERROR_MESSAGE);
        return mockError;
    }

    private Results createResults() {
        ResultsQuestions mockResultsQuestions = mock(ResultsQuestions.class);
        when(mockResultsQuestions.getCorrect()).thenReturn(CORRECT_ANSWERS);
        when(mockResultsQuestions.getIncorrect()).thenReturn(INCORRECT_ANSWERS);
        when(mockResultsQuestions.getAsked()).thenReturn(QUESTIONS_ASKED);
        ArrayOfString mockTransIds = mock(ArrayOfString.class);
        when(mockTransIds.getString()).thenReturn(List.of(TRANS_ID));
        Results mockResults = mock(Results.class);
        when(mockResults.getAuthenticationResult()).thenReturn(AUTH_RESULT);
        when(mockResults.getConfirmationCode()).thenReturn(CONFIRMATION_CODE);
        when(mockResults.getOutcome()).thenReturn(OUTCOME);
        when(mockResults.getQuestions()).thenReturn(mockResultsQuestions);
        when(mockResults.getNextTransId()).thenReturn(mockTransIds);
        return mockResults;
    }

    private Questions createQuestions() {
        AnswerFormat mockAnswerFormat = mock(AnswerFormat.class);
        when(mockAnswerFormat.getFieldType()).thenReturn(FIELD_TYPE);
        when(mockAnswerFormat.getIdentifier()).thenReturn(ANSWER_FORMAT_ID);
        when(mockAnswerFormat.getAnswerList()).thenReturn(List.of(OPTION_ONE, OPTION_TWO));
        Questions mockQuestions = mock(Questions.class);
        Question mockQuestion = mock(Question.class);
        when(mockQuestion.getQuestionID()).thenReturn(QUESTION_ID);
        when(mockQuestion.getText()).thenReturn(QUESTION_TEXT);
        when(mockQuestion.getTooltip()).thenReturn(QUESTION_TOOLTIP);
        when(mockQuestion.getAnswerFormat()).thenReturn(mockAnswerFormat);
        when(mockQuestions.getQuestion()).thenReturn(List.of(mockQuestion));
        return mockQuestions;
    }
}
