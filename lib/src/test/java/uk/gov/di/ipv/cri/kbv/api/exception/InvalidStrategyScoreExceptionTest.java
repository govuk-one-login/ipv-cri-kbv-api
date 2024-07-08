package uk.gov.di.ipv.cri.kbv.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidStrategyScoreExceptionTest {

    @Test
    void shouldReturnDefaultExceptionMessage() {
        InvalidStrategyScoreException exception =
                assertThrows(
                        InvalidStrategyScoreException.class,
                        () -> {
                            throw new InvalidStrategyScoreException();
                        });
        assertEquals("No question strategy found for score provided", exception.getMessage());
    }

    @Test
    void shouldReturnCustomExceptionMessage() {
        InvalidStrategyScoreException exception =
                assertThrows(
                        InvalidStrategyScoreException.class,
                        () -> {
                            throw new InvalidStrategyScoreException("dummy");
                        });
        assertEquals("dummy", exception.getMessage());
    }
}
