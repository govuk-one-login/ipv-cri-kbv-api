package uk.gov.di.ipv.cri.kbv.api.domain;

public class QuestionAnswer {
    private String questionId;
    private String answer;

    public QuestionAnswer() {
        this(new QuestionAnswerPair());
    }

    public QuestionAnswer(QuestionAnswerPair questionAnswerPair) {
        this.questionId = questionAnswerPair.getQuestion().getQuestionId();
        this.answer = questionAnswerPair.getAnswer();
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
