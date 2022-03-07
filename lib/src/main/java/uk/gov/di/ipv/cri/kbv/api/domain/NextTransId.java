package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class NextTransId {
    @JsonProperty("string")
    private List<String> transactionValue;

    public List<String> getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(List<String> transactionValue) {
        if (transactionValue == null) {
            transactionValue = new ArrayList<>();
        }
        this.transactionValue = transactionValue;
    }
}
