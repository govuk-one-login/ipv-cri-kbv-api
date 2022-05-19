package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Optional;

public class KBVStorageService {
    private final DataStore<KBVItem> dataStore;

    public KBVStorageService(DataStore<KBVItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<KBVItem> getSessionId(String sessionId) {
        return Optional.of(dataStore.getItem(sessionId));
    }

    public void update(KBVItem kbvItem) {
        dataStore.update(kbvItem);
    }
}
