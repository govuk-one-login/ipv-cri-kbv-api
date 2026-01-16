package uk.gov.di.ipv.cri.kbv.api.exception;

public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }
}
