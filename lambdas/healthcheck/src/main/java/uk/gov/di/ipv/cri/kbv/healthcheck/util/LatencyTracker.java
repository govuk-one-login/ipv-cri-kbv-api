package uk.gov.di.ipv.cri.kbv.healthcheck.util;

public class LatencyTracker {
    private long startTime;
    private long endTime;

    public LatencyTracker() {
        reset();
    }

    public void start() {
        this.startTime = System.nanoTime();
    }

    public void stop() {
        this.endTime = System.nanoTime();
    }

    public void reset() {
        this.startTime = 0;
        this.endTime = 0;
    }

    public long getElapsedTimeInMs() {
        if (endTime == 0 || endTime < startTime) {
            throw new IllegalStateException("stop() must be called after start()");
        }
        return (endTime - startTime) / 1_000_000;
    }
}
