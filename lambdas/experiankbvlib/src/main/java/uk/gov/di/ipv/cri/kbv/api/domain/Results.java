package uk.gov.di.ipv.cri.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Alerts;

public class Results {
    private String outcome;
    private String authenticationResult;
    private NextTransId nextTransId;
    private ResultQuestions questions;
    private Alerts alerts;
    private String caseFoundFlag;
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

    public NextTransId getNextTransId() {
        return nextTransId;
    }

    public void setNextTransId(NextTransId nextTransId) {
        this.nextTransId = nextTransId;
    }

    public ResultQuestions getQuestions() {
        return questions;
    }

    public void setQuestions(ResultQuestions questions) {
        this.questions = questions;
    }

    public Alerts getAlerts() {
        return alerts;
    }

    public void setAlerts(Alerts alerts) {
        this.alerts = alerts;
    }

    public String getCaseFoundFlag() {
        return caseFoundFlag;
    }

    public void setCaseFoundFlag(String caseFoundFlag) {
        this.caseFoundFlag = caseFoundFlag;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
