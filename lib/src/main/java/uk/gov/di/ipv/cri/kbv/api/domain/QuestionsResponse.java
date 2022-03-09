package uk.gov.di.ipv.cri.kbv.api.domain;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import java.util.stream.Collectors;

public class QuestionsResponse {

    protected Control control;
    protected Questions questions;
    protected Results results;
    protected Error error;

    public Control getControl() {
        return control;
    }

    public void setControl(Control control) {
        this.control = control;
    }

    public Questions getQuestions() {
        return questions;
    }

    public void setQuestions(Questions questions) {
        this.questions = questions;
    }

    public boolean hasQuestions() {
        return this.getQuestions() != null;
    }

    public Results getResults() {
        return results;
    }

    public boolean hasQuestionRequestEnded() {
        if (StringUtils.isNotBlank(this.getQuestionStatus())) {
            return this.getQuestionStatus().equalsIgnoreCase("END");
        }
        return false;
    }

    public String getQuestionStatus() {
        return results.getNextTransId().getTransactionValue().stream()
                .collect(Collectors.joining(""));
    }

    public String getStatus() {
        return results.getAuthenticationResult();
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
