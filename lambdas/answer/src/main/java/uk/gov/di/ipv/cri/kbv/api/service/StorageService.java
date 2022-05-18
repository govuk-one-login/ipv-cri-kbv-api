package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;

import java.util.Optional;

public class StorageService {
    private final DataStore<SessionItem> dataStore;

    public StorageService(DataStore<SessionItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<SessionItem> getSessionId(String sessionId) {
        return Optional.of(dataStore.getItem(sessionId));
    }

    public void update(SessionItem kbvSessionItem) {
        dataStore.update(kbvSessionItem);
    }
}
