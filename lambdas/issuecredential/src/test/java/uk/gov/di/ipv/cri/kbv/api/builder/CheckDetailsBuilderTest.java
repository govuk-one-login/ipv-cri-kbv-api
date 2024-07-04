package uk.gov.di.ipv.cri.kbv.api.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CheckDetailsBuilderTest implements TestFixtures {
    private QuestionState questionState;
    private KBVItem kbvItem;
    private Map<String, Integer> kbvQualityQuestionMapping =
            Map.of(
                    "FirstQuestion", 9,
                    "SecondQuestion", 3,
                    "ThirdQuestion", 7,
                    "FourthQuestion", 4);
    private CheckDetailsBuilder builder;

    @Test
    @DisplayName(
            "On CheckDetailsBuilder creation only the following methods in the test are available i.e. ")
    void whenInitIsCalledTheStepHasTwoPossibleMethods() {
        CheckDetailsBuilder checkDetailsBuilder =
                new CheckDetailsBuilder(
                        new QuestionState().getAllQaPairs(),
                        new KbvQuestionAnswerSummary().getAnsweredCorrect(),
                        Map.of("", 0));

        assertNotNull(checkDetailsBuilder);
        assertNotNull(checkDetailsBuilder.getQuestionIdsInBatches());
        assertNotNull(checkDetailsBuilder.skip1stQuestionIdIn2ndBatch());
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {

        kbvItem = getKbvItem();
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 3, 1));
        setKbvItemQuestionState(
                kbvItem, "FirstQuestion", "SecondQuestion", "ThirdQuestion", "FourthQuestion");

        List<KbvQuestion> kbvQuestionsFirstSecond =
                getKbvQuestions("FirstQuestion", "SecondQuestion");
        List<QuestionAnswer> questionFirstSecondAnswers =
                getQuestionAnswers("FirstQuestion", "SecondQuestion");

        List<KbvQuestion> kbvQuestionsThird = getKbvQuestions("ThirdQuestion");
        List<QuestionAnswer> questionThirdAnswers = getQuestionAnswers("ThirdQuestion");

        List<KbvQuestion> kbvQuestionsFourth = getKbvQuestions("FourthQuestion");
        List<QuestionAnswer> questionFourthAnswers = getQuestionAnswers("FourthQuestion");

        questionState = new QuestionState();
        questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));

        questionState =
                loadKbvQuestionStateWithAnswers(
                        questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

        questionState.setQAPairs(kbvQuestionsThird.toArray(KbvQuestion[]::new));
        questionState =
                loadKbvQuestionStateWithAnswers(
                        questionState, kbvQuestionsThird, questionThirdAnswers);

        questionState.setQAPairs(kbvQuestionsFourth.toArray(KbvQuestion[]::new));
        questionState =
                loadKbvQuestionStateWithAnswers(
                        questionState, kbvQuestionsFourth, questionFourthAnswers);

        kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));

        builder =
                new CheckDetailsBuilder(
                        questionState.getAllQaPairs(),
                        kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect(),
                        kbvQualityQuestionMapping);
    }

    @Test
    @DisplayName(
            " getQuestionIdsInAllBatches returns all QuestionId(s) for all batches questions received")
    void returnsAllQuestionIdsInAllBatches() {
        List<String> questionIds = builder.getQuestionIdsInBatches().buildToList();

        assertAll(
                () -> assertEquals(4, questionIds.size()),
                () -> assertEquals("FirstQuestion", questionIds.get(0)),
                () -> assertEquals("SecondQuestion", questionIds.get(1)),
                () -> assertEquals("ThirdQuestion", questionIds.get(2)),
                () -> assertEquals("FourthQuestion", questionIds.get(3)));
    }

    @Test
    @DisplayName(
            "Get un-skipped question Ids returns all the other questions; assuming the first two questions requested are correct, then first question of the next batch of question is skipped")
    void returnsTheRemainingQuestionIdsInAllBatchesWhenQuestionIdIn2ndBatchIsSkipped() {
        List<String> questionIds =
                builder.skip1stQuestionIdIn2ndBatch()
                        .getUnSkippedQuestionIdsInBatches()
                        .buildToList();

        assertAll(
                () -> assertEquals(3, questionIds.size()),
                () -> assertEquals("FirstQuestion", questionIds.get(0)),
                () -> assertEquals("SecondQuestion", questionIds.get(1)),
                () -> assertEquals("FourthQuestion", questionIds.get(2)));
    }

    @Test
    @DisplayName(
            "It maps questionId(s) exactly with there corresponding kbvQuality values, when all questions received are assumed to have passed")
    void returnKbvQualityAssociatedWithQuestionIdInAllBatchesInTheExpectedOrder() {
        CheckDetail[] checkDetails =
                builder.getQuestionIdsInBatches()
                        .createCheckDetailsWithKbvQuality()
                        .filterByNumberOfCorrectQuestions()
                        .buildToArray();

        assertAll(
                () -> assertEquals(3, checkDetails.length),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("FirstQuestion"),
                                checkDetails[0].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("SecondQuestion"),
                                checkDetails[1].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("ThirdQuestion"),
                                checkDetails[2].getKbvQuality()));
    }

    @Test
    @DisplayName(
            "It maps questionId(s) exactly with there corresponding kbvQuality values, when the wrong question has been excluded received are assumed to have passed")
    void returnsKbvQualityAssociatedWithQuestionIdInAllBatchesInTheExpectedOrder() {
        CheckDetail[] checkDetails =
                builder.skip1stQuestionIdIn2ndBatch()
                        .getUnSkippedQuestionIdsInBatches()
                        .createCheckDetailsWithKbvQuality()
                        .buildToArray();

        assertAll(
                () -> assertEquals(3, checkDetails.length),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("FirstQuestion"),
                                checkDetails[0].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("SecondQuestion"),
                                checkDetails[1].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("FourthQuestion"),
                                checkDetails[2].getKbvQuality()));
    }

    @Test
    @DisplayName(
            "Assuming any one of the two initial question is wrong, it would map kbv quality values from the lowest to the highest")
    void returnsKbvQualitySortedFromLowestToHighest() {
        CheckDetail[] checkDetails =
                builder.getQuestionIdsInBatches()
                        .createCheckDetailsWithKbvQuality()
                        .sortByKbvQualityFromLowestToHighest()
                        .filterByNumberOfCorrectQuestions()
                        .buildToArray();

        assertAll(
                () -> assertEquals(3, checkDetails.length),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("SecondQuestion"),
                                checkDetails[0].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("FourthQuestion"),
                                checkDetails[1].getKbvQuality()),
                () ->
                        assertEquals(
                                kbvQualityQuestionMapping.get("ThirdQuestion"),
                                checkDetails[2].getKbvQuality()));
    }
}
