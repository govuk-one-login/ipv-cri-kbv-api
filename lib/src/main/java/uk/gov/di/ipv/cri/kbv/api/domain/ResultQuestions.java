package uk.gov.di.ipv.cri.kbv.api.domain;

public class ResultQuestions {
    private String asked;
    private String correct;
    private String incorrect;
    private String skipped;

    public String getAsked() {
        return asked;
    }

    public void setAsked(String asked) {
        this.asked = asked;
    }

    public String getCorrect() {
        return correct;
    }

    public void setCorrect(String correct) {
        this.correct = correct;
    }

    public String getIncorrect() {
        return incorrect;
    }

    public void setIncorrect(String incorrect) {
        this.incorrect = incorrect;
    }

    public String getSkipped() {
        return skipped;
    }

    public void setSkipped(String skipped) {
        this.skipped = skipped;
    }
}
