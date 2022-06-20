package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.exception.KbvItemNotFoundException;

import java.util.UUID;

public class KBVStorageService {
    private final DataStore<KBVItem> dataStore;

    @ExcludeFromGeneratedCoverageReport
    public KBVStorageService(ConfigurationService configurationService) {
        this.dataStore =
                new DataStore<>(
                        configurationService.getParameterValue("KBVTableName"),
                        KBVItem.class,
                        new DynamoDbEnhancedClientFactory().getClient());
    }

    public KBVStorageService(DataStore<KBVItem> datastore) {
        this.dataStore = datastore;
    }

    public KBVItem getKBVItem(UUID sessionId) {
        KBVItem kbvItem = this.dataStore.getItem(String.valueOf(sessionId));
        if (kbvItem != null) {
            return kbvItem;
        }
        throw new KbvItemNotFoundException("KBV Item not Found.");
    }

    public void update(KBVItem kbvItem) {
        dataStore.update(kbvItem);
    }

    public void save(KBVItem kbvItem) {
        dataStore.create(kbvItem);
    }
}
