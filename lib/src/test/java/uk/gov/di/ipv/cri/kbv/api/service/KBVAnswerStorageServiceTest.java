package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.util.KBVAnswerStorageMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getExperianQuestionResponse;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionOne;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionTwo;

@ExtendWith(MockitoExtension.class)
class KBVAnswerStorageServiceTest {
    @Mock private DataStore mockDataStore;
    @Mock private KBVAnswerStorageMapper mockKBVAnswerMapper;
    @InjectMocks private KBVAnswerStorageService kbvAnswerStorageService;

    @Test
    void shouldSaveQuestionAnswerBatches() {
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(new KbvQuestion[] {getQuestionOne(), getQuestionTwo()});

        List<KBVAnswerItem> mockKVBAnswerItems = mock(List.class);
        when(mockKBVAnswerMapper.mapToKBVAnswerItems(questionsResponse))
                .thenReturn(mockKVBAnswerItems);
        kbvAnswerStorageService.save(questionsResponse);

        verify(mockKBVAnswerMapper).mapToKBVAnswerItems(questionsResponse);
        verify(mockDataStore).createItems(mockKVBAnswerItems);
    }

    @Test
    void shouldNotSaveKbvAnswerItemsWhenQuestionResponseHasNoQuestions() {
        kbvAnswerStorageService.save(new QuestionsResponse());

        verify(mockDataStore, never()).createItems(any());
    }
}
