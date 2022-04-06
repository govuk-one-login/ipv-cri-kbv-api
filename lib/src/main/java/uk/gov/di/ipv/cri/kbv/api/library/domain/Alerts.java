package uk.gov.di.ipv.cri.kbv.api.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Alerts {

    @JsonProperty("alerts")
    List<String> alertMessages;

    public List<String> getAlertMessages() {
        return alertMessages;
    }

    public void setAlertMessages(List<String> alertMessages) {
        this.alertMessages = alertMessages;
    }
}
