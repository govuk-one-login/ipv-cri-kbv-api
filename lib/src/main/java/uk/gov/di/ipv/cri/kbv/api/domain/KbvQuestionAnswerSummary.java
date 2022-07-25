package uk.gov.di.ipv.cri.kbv.api.domain;

public class KbvQuestionAnswerSummary {
    private int questionsAsked;
    private int answeredCorrect;
    private int answeredIncorrect;
    private int questionsSkipped;

    public int getQuestionsAsked() {
        return questionsAsked;
    }

    public void setQuestionsAsked(int questionsAsked) {
        this.questionsAsked = questionsAsked;
    }

    public int getAnsweredCorrect() {
        return answeredCorrect;
    }

    public void setAnsweredCorrect(int answeredCorrect) {
        this.answeredCorrect = answeredCorrect;
    }

    public int getAnsweredIncorrect() {
        return answeredIncorrect;
    }

    public void setAnsweredIncorrect(int answeredIncorrect) {
        this.answeredIncorrect = answeredIncorrect;
    }

    public int getQuestionsSkipped() {
        return questionsSkipped;
    }

    public void setQuestionsSkipped(int questionsSkipped) {
        this.questionsSkipped = questionsSkipped;
    }
}
