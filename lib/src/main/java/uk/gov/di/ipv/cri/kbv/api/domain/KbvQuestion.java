package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KbvQuestion {
    @JsonProperty("questionID")
    private String questionId;

    private String text;
    private String tooltip;

    @JsonProperty("answerFormat")
    private KbvQuestionOptions questionOptions;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public KbvQuestionOptions getQuestionOptions() {
        return questionOptions;
    }

    public void setQuestionOptions(KbvQuestionOptions questionOptions) {
        this.questionOptions = questionOptions;
    }
}
