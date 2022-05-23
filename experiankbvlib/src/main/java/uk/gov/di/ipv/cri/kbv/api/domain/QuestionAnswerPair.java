package uk.gov.di.ipv.cri.kbv.api.domain;

public class QuestionAnswerPair {

    private com.experian.uk.schema.experian.identityiq.services.webservice.Question question;
    private String answer;

    public QuestionAnswerPair() {}

    public QuestionAnswerPair(
            com.experian.uk.schema.experian.identityiq.services.webservice.Question question) {
        this.question = question;
    }

    public com.experian.uk.schema.experian.identityiq.services.webservice.Question getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
