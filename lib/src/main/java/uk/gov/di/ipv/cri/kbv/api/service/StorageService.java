package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

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

    public void update(String sessionId, String state, String auth, String urn) {
        KBVSessionItem kbvSessionItem = getKbvSessionItem(sessionId, state);
        kbvSessionItem.setAuthRefNo(auth);
        kbvSessionItem.setUrn(urn);
        dataStore.update(kbvSessionItem);
    }

    private KBVSessionItem getKbvSessionItem(String sessionId, String state) {
        KBVSessionItem kbvSessionItem = dataStore.getItem(sessionId);
        kbvSessionItem.setQuestionState(state);
        return kbvSessionItem;
    }

    public void updateAuthorisationCode(String sessionId, String state) {
        KBVSessionItem kbvSessionItem = getKbvSessionItem(sessionId, state);
        kbvSessionItem.setAuthorizationCode(UUID.randomUUID().toString());
        dataStore.update(kbvSessionItem);
    }
}
