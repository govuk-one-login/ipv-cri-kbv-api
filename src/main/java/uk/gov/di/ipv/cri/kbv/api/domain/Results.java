package uk.gov.di.ipv.cri.kbv.api.domain;

import java.util.ArrayList;
import java.util.List;

public class Results {
    private String outcome;
    private String authenticationResult;
    List<String> nextTransId;

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getAuthenticationResult() {
        return authenticationResult;
    }

    public void setAuthenticationResult(String authenticationResult) {
        this.authenticationResult = authenticationResult;
    }

    public List<String> getNextTransId() {
        if (nextTransId == null) {
            nextTransId = new ArrayList<String>();
        }
        return this.nextTransId;
    }
}
