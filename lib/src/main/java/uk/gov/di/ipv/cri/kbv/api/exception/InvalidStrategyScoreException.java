package uk.gov.di.ipv.cri.kbv.api.exception;

public class InvalidStrategyScoreException extends RuntimeException {
    public InvalidStrategyScoreException(String message) {
        super(message);
    }

    public InvalidStrategyScoreException() {
        this("No question strategy found for score provided");
    }
}
