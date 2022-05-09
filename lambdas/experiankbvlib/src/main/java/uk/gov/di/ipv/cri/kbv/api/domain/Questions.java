package uk.gov.di.ipv.cri.kbv.api.domain;

public class Questions {

    private Question[] question;
    private int skipsRemaining;
    private String skipWarning;

    public Question[] getQuestion() {
        return question;
    }

    public int getSkipsRemaining() {
        return skipsRemaining;
    }

    public String getSkipWarning() {
        return skipWarning;
    }
}
