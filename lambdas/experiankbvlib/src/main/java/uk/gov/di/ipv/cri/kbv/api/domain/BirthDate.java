package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BirthDate {

    @JsonProperty("value")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
