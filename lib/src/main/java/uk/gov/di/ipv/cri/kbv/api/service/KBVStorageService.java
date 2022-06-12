package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.UUID;

import static uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants.KBV_TABLE_NAME;

public class KBVStorageService {
    private final DataStore<KBVItem> dataStore;

    public KBVStorageService() {
        this(
                new DataStore<>(
                        new AWSParamStoreRetriever().getValue(KBV_TABLE_NAME),
                        KBVItem.class,
                        new DynamoDbEnhancedClientFactory().getClient()));
    }

    public KBVStorageService(DataStore<KBVItem> datastore) {
        this.dataStore = datastore;
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
