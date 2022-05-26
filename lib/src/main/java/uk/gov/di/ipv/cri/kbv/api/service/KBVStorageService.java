package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Objects;
import java.util.Optional;

public class KBVStorageService {
    private final DataStore<KBVItem> dataStore;

    public KBVStorageService() {
        this.dataStore =
                new DataStore<KBVItem>(getKBVTableName(), KBVItem.class, DataStore.getClient());
    }

    public KBVStorageService(DataStore<KBVItem> datastore) {
        this.dataStore = datastore;
    }

    public Optional<KBVItem> getSessionId(String sessionId) {
        return Optional.of(this.dataStore.getItem(sessionId));
    }

    public KBVItem getKBVItem(String sessionId) {
        return this.dataStore.getItem(sessionId);
    }

    public void update(KBVItem kbvItem) {
        dataStore.update(kbvItem);
    }

    public void save(KBVItem kbvItem) {
        dataStore.create(kbvItem);
    }

    public String getKBVTableName() {
        return ParamManager.getSsmProvider().get(getParameterName("KBVTableName"));
    }

    public String getParameterName(String parameterName) {
        var awstStackName = System.getenv("AWS_STACK_NAME");
        var parameterPrefix =
                Objects.requireNonNull(awstStackName, "env var AWS_STACK_NAME required");

        return String.format("/%s/%s", parameterPrefix, parameterName);
    }
}
