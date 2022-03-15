package uk.gov.di.ipv.cri.kbv.api.domain;

public class Question {
    private String questionID;
    private String text;
    private String tooltip;
    private String answerHeldFlag;
    private AnswerFormat answerFormat;

    public void setQuestionID(String value) {
        this.questionID = value;
    }

    public String getQuestionID() {
        return questionID;
    }

    public void setText(String value) {
        this.text = value;
    }

    public String getText() {
        return text;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getAnswerHeldFlag() {
        return answerHeldFlag;
    }

    public AnswerFormat getAnswerFormat() {
        return answerFormat;
    }
}
