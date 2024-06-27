package uk.gov.di.ipv.cri.kbv.api.strategy;

public class Strategy {
    private final int min;
    private final int max;

    public Strategy(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}
