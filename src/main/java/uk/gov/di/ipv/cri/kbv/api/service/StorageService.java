package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

public class StorageService {
    private final DataStore<KBVSessionItem> dataStore;

    public StorageService(DataStore<KBVSessionItem> datastore) {
        this.dataStore = datastore;
    }

    public KBVSessionItem getSessionId(String sessionId) {
        return dataStore.getItem(sessionId);
    }

    public void save(String sessionId, String state) {
        KBVSessionItem kbvSessionItem = new KBVSessionItem();
        kbvSessionItem.setSessionId(sessionId);
        kbvSessionItem.setQuestionState(state);
        dataStore.create(kbvSessionItem);
    }
}
