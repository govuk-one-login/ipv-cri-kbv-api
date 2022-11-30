package uk.gov.di.ipv.cri.kbv.api.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getExperianQuestionResponse;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionOne;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionTwo;

@ExtendWith(MockitoExtension.class)
class QuestionStateTest {

    private QuestionState questionState;

    @BeforeEach
    void setUp() {
        questionState = new QuestionState();
    }

    @Test
    void shouldEvaluateToTrueWhenAllQuestionsHaveAnswers() {
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getAnswer()).thenReturn("answer-1");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock2.getAnswer()).thenReturn("answer-2");
        questionState
                .getQaPairs()
                .addAll(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        boolean allQuestionsAnswered = questionState.questionsHaveAllBeenAnswered();
        assertTrue(allQuestionsAnswered);
    }

    @Test
    void shouldEvaluateToFalseWhenAtLeastOneQuestionsIsUnAnswered() {
        QuestionAnswerPair questionAnswerPairMock1 = mock(QuestionAnswerPair.class);
        when(questionAnswerPairMock1.getAnswer()).thenReturn("answer-1");
        QuestionAnswerPair questionAnswerPairMock2 = mock(QuestionAnswerPair.class);
        questionState
                .getQaPairs()
                .addAll(List.of(questionAnswerPairMock1, questionAnswerPairMock2));

        boolean allQuestionsAnswered = questionState.questionsHaveAllBeenAnswered();
        assertFalse(allQuestionsAnswered);
    }

    @Test
    void shouldEvaluateToTrueWhenQuestionsResponseHasQuestions() {
        QuestionsResponse questionsResponseMock = mock(QuestionsResponse.class);
        when(questionsResponseMock.getQuestions())
                .thenReturn(new KbvQuestion[] {new KbvQuestion()});
        when(questionsResponseMock.hasQuestions()).thenReturn(Boolean.TRUE);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponseMock);
        assertTrue(hasMoreQuestions);
    }

    @Test
    void shouldEvaluateToFalseWhenQuestionResponseHasNoQuestion() {
        QuestionsResponse questionsResponse = mock(QuestionsResponse.class);
        when(questionsResponse.hasQuestions()).thenReturn(Boolean.FALSE);

        boolean hasMoreQuestions = questionState.setQuestionsResponse(questionsResponse);
        assertFalse(hasMoreQuestions);
    }

    @Test
    void shouldReduceArray() {
        List<String> sample = List.of("END");
        String result = sample.stream().reduce("", String::concat);

        assertEquals("END", result);
    }

    @Test
    void questionStateShouldRetainOldQuestionsWhenNewQuestionsAreSetInBatchesOf2And2() {
        var questionOne = getQuestionOne();
        var questionTwo = getQuestionTwo();
        var questionThree = getQuestionOne();
        var questionFour = getQuestionTwo();
        QuestionsResponse questionsResponse1 =
                getExperianQuestionResponse(new KbvQuestion[] {questionOne, questionTwo});
        QuestionsResponse questionsResponse2 =
                getExperianQuestionResponse(new KbvQuestion[] {questionThree, questionFour});

        questionState.setQAPairs(questionsResponse1.getQuestions());
        questionState.setQAPairs(questionsResponse2.getQuestions());

        assertTrue(questionState.allQuestionBatchSizesMatch(2));
        assertEquals(4, questionState.getQuestionIdsFromQAPairs().count());
    }

    @Test
    void questionStateShouldRetainOldQuestionsWhenNewQuestionsAreSetInBatchOf2by1by1() {
        var questionOne = getQuestionOne();
        var questionTwo = getQuestionTwo();
        var questionThree = getQuestionOne();
        var questionFour = getQuestionTwo();
        QuestionsResponse questionsResponse12 =
                getExperianQuestionResponse(new KbvQuestion[] {questionOne, questionTwo});
        QuestionsResponse questionsResponse3 =
                getExperianQuestionResponse(new KbvQuestion[] {questionThree});
        QuestionsResponse questionsResponse4 =
                getExperianQuestionResponse(new KbvQuestion[] {questionFour});
        questionState.setQAPairs(questionsResponse12.getQuestions());
        questionState.setQAPairs(questionsResponse3.getQuestions());
        questionState.setQAPairs(questionsResponse4.getQuestions());

        assertFalse(questionState.allQuestionBatchSizesMatch(2));
        assertEquals(4, questionState.getQuestionIdsFromQAPairs().count());
    }

    @Test
    void questionStateShouldfilterOutQAPairAtIndexOne() {
        var questionOne = getQuestionOne();
        var questionTwo = getQuestionTwo();
        var questionThree = getQuestionOne();
        var questionFour = getQuestionTwo();
        QuestionsResponse questionsResponse12 =
                getExperianQuestionResponse(new KbvQuestion[] {questionOne, questionTwo});
        QuestionsResponse questionsResponse3 =
                getExperianQuestionResponse(new KbvQuestion[] {questionThree});
        QuestionsResponse questionsResponse4 =
                getExperianQuestionResponse(new KbvQuestion[] {questionFour});
        questionState.setQAPairs(questionsResponse12.getQuestions());
        questionState.setQAPairs(questionsResponse3.getQuestions());
        questionState.setQAPairs(questionsResponse4.getQuestions());

        assertFalse(questionState.allQuestionBatchSizesMatch(2));
        assertEquals(2, questionState.skipQaPairAtIndexOne().count());
    }
}
