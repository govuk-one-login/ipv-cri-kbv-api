package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class QuestionAnswerRequest {
    @NotBlank(message = "{questionAnswerRequest.urn.required}")
    private String urn;

    @NotBlank(message = "{questionAnswerRequest.authRefNo.required}")
    private String authRefNo;

    @Valid
    @NotNull(message = "{questionAnswerRequest.questionAnswer.required}")
    @NotEmpty(message = "{questionAnswerRequest.questionAnswers.required}")
    private List<QuestionAnswer> questionAnswers;

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public void setAuthRefNo(String authRefNo) {
        this.authRefNo = authRefNo;
    }

    public void setQuestionAnswers(List<QuestionAnswer> questionAnswers) {
        this.questionAnswers = questionAnswers;
    }

    public String getUrn() {
        return urn;
    }

    public String getAuthRefNo() {
        return authRefNo;
    }

    public List<QuestionAnswer> getQuestionAnswers() {
        return questionAnswers;
    }

    @JsonIgnore
    public String getAllQuestionIdAnswered() {
        return this.getQuestionAnswers().stream()
                .map(QuestionAnswer::getQuestionId)
                .collect(Collectors.joining(","));
    }
}
