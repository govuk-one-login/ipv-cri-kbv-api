package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public enum KbvQuality {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int value;

    private KbvQuality(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
