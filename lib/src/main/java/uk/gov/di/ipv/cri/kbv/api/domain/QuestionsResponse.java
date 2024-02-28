package uk.gov.di.ipv.cri.kbv.api.domain;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import java.util.Objects;

public class QuestionsResponse {
    private String uniqueReference;
    private String authReference;
    private String errorCode;
    private String errorMessage;
    private KbvQuestion[] questions;
    private KbvResult results;

    public String getUniqueReference() {
        return uniqueReference;
    }

    public void setUniqueReference(String uniqueReference) {
        this.uniqueReference = uniqueReference;
    }

    public String getAuthReference() {
        return authReference;
    }

    public void setAuthReference(String authReference) {
        this.authReference = authReference;
    }

    public KbvQuestion[] getQuestions() {
        return questions;
    }

    public void setQuestions(KbvQuestion[] questions) {
        this.questions = questions;
    }

    public boolean hasQuestions() {
        return Objects.nonNull(this.getQuestions()) && this.getQuestions().length > 0;
    }

    public KbvResult getResults() {
        return results;
    }

    public boolean hasQuestionRequestEnded() {
        return Objects.nonNull(this.getQuestionStatus())
                && this.getQuestionStatus().equalsIgnoreCase("END");
    }

    public String getQuestionStatus() {
        return Objects.nonNull(results) && Objects.nonNull(results.getNextTransId())
                ? String.join("", results.getNextTransId())
                : null;
    }

    public String getStatus() {
        return Objects.nonNull(results) ? results.getAuthenticationResult() : null;
    }

    public boolean isThinFile() {
        return Objects.nonNull(this.getStatus())
                && this.getStatus().equalsIgnoreCase("Unable to Authenticate");
    }

    public void setResults(KbvResult results) {
        this.results = results;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return StringUtils.isNotBlank(this.errorMessage) || StringUtils.isNotBlank(this.errorCode);
    }

    public KbvQuestionAnswerSummary getQuestionAnswerResultSummary() {
        if (Objects.nonNull(this.results)
                && Objects.nonNull(this.getResults().getAnswerSummary())) {
            return this.getResults().getAnswerSummary();
        }
        return null;
    }
}
