package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.util.KBVAnswerStorageMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getExperianQuestionResponse;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionOne;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.getQuestionTwo;

@ExtendWith(MockitoExtension.class)
class KBVAnswerStorageServiceTest {
    @Mock DynamoDbEnhancedClient mockEnhancedClient;
    @Mock KBVAnswerStorageMapper mockKBVAnswerMapper;

    private KBVAnswerStorageService kbvAnswerStorageService;

    @BeforeEach
    void setUp() {
        kbvAnswerStorageService =
                new KBVAnswerStorageService(
                        mockEnhancedClient, mockKBVAnswerMapper, "KBVAnswerTable");
    }

    @Test
    void shouldSaveQuestionAnswerBatches() {
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(new KbvQuestion[] {getQuestionOne(), getQuestionTwo()});

        kbvAnswerStorageService.save(questionsResponse);

        verify(mockEnhancedClient).table(eq("KBVAnswerTable"), any(BeanTableSchema.class));
        verify(mockKBVAnswerMapper).mapToKBVAnswerItems(questionsResponse);
        verify(mockEnhancedClient).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
    }

    @Test
    void shouldDoNothingWhenQuestionAnswerBatchesReceivesAnEmptyList() {
        QuestionsResponse questionsResponse =
                getExperianQuestionResponse(
                        new KbvQuestion[] {new KbvQuestion(), new KbvQuestion()});

        kbvAnswerStorageService.save(questionsResponse);

        assertEquals(
                Collections.emptyList(),
                mockKBVAnswerMapper.mapToKBVAnswerItems(questionsResponse));
        verify(mockEnhancedClient).table(eq("KBVAnswerTable"), any(BeanTableSchema.class));
        verify(mockKBVAnswerMapper, times(2)).mapToKBVAnswerItems(questionsResponse);
        verify(mockEnhancedClient).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
    }
}
