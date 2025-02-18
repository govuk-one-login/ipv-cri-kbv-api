package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Optional;
import java.util.UUID;

public class KBVStorageService {
    private final DataStore<KBVItem> dataStore;

    @ExcludeFromGeneratedCoverageReport
    public KBVStorageService(
            ConfigurationService configurationService,
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dataStore =
                new DataStore<>(
                        configurationService.getParameterValue("KBVTableName"),
                        KBVItem.class,
                        dynamoDbEnhancedClient);
    }

    public KBVStorageService(DataStore<KBVItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<KBVItem> getSessionId(String sessionId) {
        return Optional.of(this.dataStore.getItem(sessionId));
    }

    public KBVItem getKBVItem(UUID sessionId) {
        return this.dataStore.getItem(String.valueOf(sessionId));
    }

    public void update(KBVItem kbvItem) {
        dataStore.update(kbvItem);
    }

    public void save(KBVItem kbvItem) {
        dataStore.create(kbvItem);
    }
}
