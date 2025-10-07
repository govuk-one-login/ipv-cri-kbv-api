package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Optional;
import java.util.UUID;

public class KBVStorageService {
    private static final String KBV_TABLE_NAME =
            Optional.ofNullable(System.getenv("KBV_TABLE_NAME")).orElse("kbv-kbv-cri-api-v1");

    private final DataStore<KBVItem> dataStore;

    @ExcludeFromGeneratedCoverageReport
    public KBVStorageService(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this(new DataStore<>(KBV_TABLE_NAME, KBVItem.class, dynamoDbEnhancedClient));
    }

    KBVStorageService(DataStore<KBVItem> dataStore) {
        this.dataStore = dataStore;
    }

    public Optional<KBVItem> getSessionId(String sessionId) {
        return Optional.ofNullable(this.dataStore.getItem(sessionId));
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
