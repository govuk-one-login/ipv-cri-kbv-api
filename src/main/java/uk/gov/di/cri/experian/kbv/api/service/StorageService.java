package uk.gov.di.cri.experian.kbv.api.service;

import uk.gov.di.cri.experian.kbv.api.domain.QuestionState;

import java.util.HashMap;
import java.util.Map;

public class StorageService {

    private Map<String, QuestionState> map = new HashMap<>();

    public void save(String key, QuestionState state) {
        map.put(key, state);
    }

    public QuestionState get(String sessionId) {
        return map.get(sessionId);
    }
}
