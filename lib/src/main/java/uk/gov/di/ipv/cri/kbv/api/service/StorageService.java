package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class StorageService {
    private final DataStore<KBVSessionItem> dataStore;
    private KBVSessionItem kbvSessionItem = new KBVSessionItem();

    public StorageService(DataStore<KBVSessionItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<KBVSessionItem> getSessionId(String sessionId) {
        return Optional.of(dataStore.getItem(sessionId));
    }

    public void save(String sessionId, String personIdentity, String questionState) {
        kbvSessionItem.setSessionId(sessionId);
        kbvSessionItem.setUserAttributes(personIdentity);
        kbvSessionItem.setQuestionState(questionState);
        kbvSessionItem.setExpiryDate(
                String.valueOf(Instant.now().plus(48, ChronoUnit.HOURS).getEpochSecond()));
        dataStore.create(kbvSessionItem);
    }

    public void update(String sessionId, String state, String auth, String urn) {
        KBVSessionItem kbvSessionItem = dataStore.getItem(sessionId);
        kbvSessionItem.setQuestionState(state);
        kbvSessionItem.setAuthRefNo(auth);
        kbvSessionItem.setUrn(urn);
        dataStore.update(kbvSessionItem);
    }
}
