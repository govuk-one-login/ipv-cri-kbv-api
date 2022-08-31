package uk.gov.di.ipv.cri.kbv.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getExperianQuestionResponse;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionOne;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionTwo;

@ExtendWith(MockitoExtension.class)
class KBVAnswerStorageMapperTest {
    @Test
    void shouldMapQuestionResponseQuestionOptionsToKBVAnswerItems() {
        KbvQuestion questionOne = getQuestionOne();
        KbvQuestion questionTwo = getQuestionTwo();
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(new KbvQuestion[] {questionOne, questionTwo});
        KBVAnswerStorageMapper kbvAnswerStorageMapper = new KBVAnswerStorageMapper();
        List<KBVAnswerItem> kbvAnswerServices =
                kbvAnswerStorageMapper.mapToKBVAnswerItems(questionsResponse);

        assertAll(
                () ->
                        assertEquals(
                                questionOne.getQuestionId(),
                                kbvAnswerServices.get(0).getQuestionId()),
                () ->
                        assertEquals(
                                questionOne.getQuestionOptions().getIdentifier(),
                                kbvAnswerServices.get(0).getIdentifier()),
                () ->
                        assertEquals(
                                questionOne.getQuestionOptions().getOptions().get(0),
                                kbvAnswerServices.get(0).getAnswer()),
                () ->
                        assertEquals(
                                questionOne.getQuestionOptions().getOptions().get(1),
                                kbvAnswerServices.get(1).getAnswer()),
                () ->
                        assertEquals(
                                questionOne.getQuestionOptions().getOptions().get(2),
                                kbvAnswerServices.get(2).getAnswer()),
                () ->
                        assertEquals(
                                questionOne.getQuestionOptions().getOptions().get(3),
                                kbvAnswerServices.get(3).getAnswer()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionId(),
                                kbvAnswerServices.get(4).getQuestionId()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionOptions().getIdentifier(),
                                kbvAnswerServices.get(4).getIdentifier()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionOptions().getOptions().get(0),
                                kbvAnswerServices.get(4).getAnswer()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionOptions().getOptions().get(1),
                                kbvAnswerServices.get(5).getAnswer()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionOptions().getOptions().get(2),
                                kbvAnswerServices.get(6).getAnswer()),
                () ->
                        assertEquals(
                                questionTwo.getQuestionOptions().getOptions().get(3),
                                kbvAnswerServices.get(7).getAnswer()));
    }

    @Test
    void shouldReturnAnEmptyCollectionOfKBVAnswerItemsWhenThereNoOptions() {
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(
                        new KbvQuestion[] {new KbvQuestion(), new KbvQuestion()});
        KBVAnswerStorageMapper kbvAnswerStorageMapper = new KBVAnswerStorageMapper();
        List<KBVAnswerItem> kbvAnswerServices =
                kbvAnswerStorageMapper.mapToKBVAnswerItems(questionsResponse);

        assertEquals(Collections.emptyList(), kbvAnswerServices);
    }
}
