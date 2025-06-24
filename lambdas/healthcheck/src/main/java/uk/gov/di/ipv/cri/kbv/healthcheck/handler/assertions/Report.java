package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions;

import java.util.HashMap;
import java.util.Map;

public class Report {
    private boolean passed;
    private final Map<String, Map<String, Object>> attributes;

    public Report() {
        this.attributes = new HashMap<>();
        this.passed = false;
    }

    public void addAttributes(String report, Map<String, Object> attributes) {
        this.attributes.put(report, attributes);
    }

    public Map<String, Map<String, Object>> getAttributes() {
        return attributes;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isPassed() {
        return passed;
    }
}
