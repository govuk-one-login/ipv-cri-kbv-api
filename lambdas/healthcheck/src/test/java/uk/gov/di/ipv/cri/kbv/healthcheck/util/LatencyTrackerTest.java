package uk.gov.di.ipv.cri.kbv.healthcheck.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatencyTrackerTest {

    @Test
    void testElapsedTimeIsMeasuredCorrectly() throws InterruptedException {
        LatencyTracker tracker = new LatencyTracker();
        tracker.start();

        Thread.sleep(100); // NOSONAR

        tracker.stop();
        long elapsed = tracker.getElapsedTimeInMs();

        assertTrue(
                elapsed >= 100 && elapsed < 200,
                "Elapsed time should be approximately 100ms, but was: " + elapsed);
    }

    @Test
    void testGetElapsedTimeThrowsExceptionIfStopNotCalled() {
        LatencyTracker tracker = new LatencyTracker();
        tracker.start();

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, tracker::getElapsedTimeInMs);
        assertEquals("stop() must be called after start()", exception.getMessage());
    }

    @Test
    void testResetClearsTimes() {
        LatencyTracker tracker = new LatencyTracker();
        tracker.start();
        tracker.stop();
        tracker.reset();

        assertThrows(IllegalStateException.class, tracker::getElapsedTimeInMs);
    }
}
