package uk.gov.di.ipv.cri.kbv.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class KBVAnswerStorageMapperTest {
    @Test
    void shouldMapQuestionResponseQuestionOptionsToKBVAnswerItems() {
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(new KbvQuestion[] {getQuestionOne(), getQuestionTwo()});
        KBVAnswerStorageMapper kbvAnswerStorageMapper = new KBVAnswerStorageMapper();
        List<KBVAnswerItem> kbvAnswerServices =
                kbvAnswerStorageMapper.mapToKBVAnswerItems(questionsResponse);

        assertAll(
                () -> assertEquals("Q00015", kbvAnswerServices.get(0).getQuestionId()),
                () -> assertEquals("UP TO £10,000", kbvAnswerServices.get(0).getAnswer()),
                () ->
                        assertEquals(
                                "OVER £35,000 UP TO £60,000", kbvAnswerServices.get(1).getAnswer()),
                () ->
                        assertEquals(
                                "OVER £60,000 UP TO £85,000", kbvAnswerServices.get(2).getAnswer()),
                () ->
                        assertEquals(
                                "NONE OF THE ABOVE / DOES NOT APPLY",
                                kbvAnswerServices.get(3).getAnswer()),
                () -> assertEquals("Q00040", kbvAnswerServices.get(4).getQuestionId()),
                () -> assertEquals("Blue", kbvAnswerServices.get(4).getAnswer()),
                () -> assertEquals("Red", kbvAnswerServices.get(5).getAnswer()),
                () -> assertEquals("Green", kbvAnswerServices.get(6).getAnswer()),
                () ->
                        assertEquals(
                                "NONE OF THE ABOVE / DOES NOT APPLY",
                                kbvAnswerServices.get(7).getAnswer()));
    }

    private QuestionsResponse getExperianQuestionResponse(KbvQuestion[] kbvQuestions) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setAuthReference("authrefno");
        questionsResponse.setUniqueReference("urn");
        questionsResponse.setQuestions(kbvQuestions);

        return questionsResponse;
    }

    private KbvQuestion getQuestionOne() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00004");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of(
                        "UP TO £10,000",
                        "OVER £35,000 UP TO £60,000",
                        "OVER £60,000 UP TO £85,000",
                        "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00015");
        question.setText("What is the outstanding balance ");
        question.setTooltip("outstanding balance tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }

    private KbvQuestion getQuestionTwo() {
        KbvQuestionOptions questionOptions = new KbvQuestionOptions();
        questionOptions.setIdentifier("A00005");
        questionOptions.setFieldType("G");
        questionOptions.setOptions(
                List.of("Blue", "Red", "Green", "NONE OF THE ABOVE / DOES NOT APPLY"));

        KbvQuestion question = new KbvQuestion();
        question.setQuestionId("Q00040");
        question.setText("What your favorite color");
        question.setTooltip("favorite color tooltip");
        question.setQuestionOptions(questionOptions);

        return question;
    }
}
