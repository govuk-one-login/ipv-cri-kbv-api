package uk.gov.di.ipv.cri.kbv.api.domain;

public class QuestionAnswerPair {

    private Question question;
    private String answer;

    public QuestionAnswerPair() {}

    public QuestionAnswerPair(Question question) {
        this.question = question;
    }

    public Question getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
