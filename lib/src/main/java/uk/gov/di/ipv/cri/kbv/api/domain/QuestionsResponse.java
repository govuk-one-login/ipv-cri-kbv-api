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
        if (StringUtils.isNotBlank(this.getQuestionStatus())) {
            return this.getQuestionStatus().equalsIgnoreCase("END");
        }
        return false;
    }

    public String getQuestionStatus() {
        return Objects.nonNull(results) && Objects.nonNull(results.getNextTransId())
                ? String.join("", results.getNextTransId())
                : null;
    }

    public String getStatus() {
        return results.getAuthenticationResult();
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
}
