package uk.gov.di.ipv.cri.kbv.api.domain;

import java.util.List;

public class KbvResult {
    private String outcome;
    private String authenticationResult;
    private List<KbvAlert> alerts;
    private KbvQuestionAnswerSummary answerSummary;
    private String[] nextTransId;
    private String confirmationCode;

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

    public List<KbvAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<KbvAlert> alerts) {
        this.alerts = alerts;
    }

    public KbvQuestionAnswerSummary getAnswerSummary() {
        return answerSummary;
    }

    public void setAnswerSummary(KbvQuestionAnswerSummary answerSummary) {
        this.answerSummary = answerSummary;
    }

    public String[] getNextTransId() {
        return nextTransId;
    }

    public void setNextTransId(String[] nextTransId) {
        this.nextTransId = nextTransId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
