package uk.gov.di.ipv.cri.kbv.api.library.domain;

public class QuestionAnswer {
    private String questionId;
    private String answer;

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
