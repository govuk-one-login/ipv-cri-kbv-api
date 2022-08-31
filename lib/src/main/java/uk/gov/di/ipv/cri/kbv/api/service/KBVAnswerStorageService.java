package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVAnswerItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.util.KBVAnswerStorageMapper;

public class KBVAnswerStorageService {
    private final DataStore<KBVAnswerItem> dataStore;
    private final KBVAnswerStorageMapper kbvAnswerStorageMapper;

    @ExcludeFromGeneratedCoverageReport
    public KBVAnswerStorageService(
            DataStore<KBVAnswerItem> dataStore, KBVAnswerStorageMapper kbvAnswerStorageMapper) {
        this.dataStore = dataStore;
        this.kbvAnswerStorageMapper = kbvAnswerStorageMapper;
    }

    public KBVAnswerStorageService(ConfigurationService configurationService) {
        this(
                new DataStore<>(
                        configurationService.getParameterValue("KBVAnswerTableName"),
                        KBVAnswerItem.class,
                        new DynamoDbEnhancedClientFactory().getClient()),
                new KBVAnswerStorageMapper());
    }

    public void save(QuestionsResponse questionsResponse) {
        if (questionsResponse.hasQuestions()) {
            dataStore.createItems(kbvAnswerStorageMapper.mapToKBVAnswerItems(questionsResponse));
        }
    }
}
