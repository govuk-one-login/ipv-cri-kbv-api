package uk.gov.di.cri.experian.kbv.api.domain;

public class Questions {

    private Question[] questions;
    private int skipsRemaining;
    private String skipsWarning;

    public Question[] getQuestion() {
        return questions;
    }

    public int getSkipsRemaining() {
        return skipsRemaining;
    }

    public String getSkipWarning() {
        return skipsWarning;
    }
}
