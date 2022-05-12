package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.library.persistence.item.KBVSessionItem;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class StorageService {
    private final DataStore<KBVSessionItem> dataStore;

    public StorageService(DataStore<KBVSessionItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<KBVSessionItem> getSessionId(String sessionId) {
        return Optional.of(dataStore.getItem(sessionId));
    }

    public void save(String sessionId, String personIdentity, String questionState) {
        KBVSessionItem kbvSessionItem = new KBVSessionItem();
        kbvSessionItem.setSessionId(sessionId);
        kbvSessionItem.setUserAttributes(personIdentity);
        kbvSessionItem.setQuestionState(questionState);
        kbvSessionItem.setExpiryDate(
                String.valueOf(Instant.now().plus(48, ChronoUnit.HOURS).getEpochSecond()));
        dataStore.create(kbvSessionItem);
    }

    public void update(KBVSessionItem kbvSessionItem) {
        dataStore.update(kbvSessionItem);
    }
}
