package uk.gov.di.ipv.cri.kbv.healthcheck.exceptions;

public class ProcessInvocationException extends RuntimeException {
    public ProcessInvocationException(String message) {
        super(message);
    }

    public ProcessInvocationException(String message, Exception exception) {
        super(message, exception);
    }
}
