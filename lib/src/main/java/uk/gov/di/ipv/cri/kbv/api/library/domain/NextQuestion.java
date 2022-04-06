package uk.gov.di.ipv.cri.kbv.api.library.domain;

public class NextQuestion {

    private boolean empty;
    private boolean present;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }
}
