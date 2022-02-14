package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.time.Instant;

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
        kbvSessionItem.setExpiryDate(String.valueOf(Instant.now().getEpochSecond()));
        dataStore.create(kbvSessionItem);
    }

    public void update(String sessionId, String state, String auth, String urn) {
        KBVSessionItem kbvSessionItem = new KBVSessionItem();
        kbvSessionItem.setSessionId(sessionId);
        kbvSessionItem.setQuestionState(state);
        kbvSessionItem.setExpiryDate(String.valueOf(Instant.now().getEpochSecond()));
        kbvSessionItem.setAuthRefNo(auth);
        kbvSessionItem.setUrn(urn);
        dataStore.update(kbvSessionItem);
    }

}
