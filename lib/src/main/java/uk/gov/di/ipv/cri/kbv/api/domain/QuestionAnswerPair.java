package uk.gov.di.ipv.cri.kbv.api.domain;

public class QuestionAnswerPair {

    private KbvQuestion question;
    private String answer;

    public QuestionAnswerPair() {}

    public QuestionAnswerPair(KbvQuestion question) {
        this.question = question;
    }

    public KbvQuestion getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
