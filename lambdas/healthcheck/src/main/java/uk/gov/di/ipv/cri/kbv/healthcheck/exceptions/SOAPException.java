package uk.gov.di.ipv.cri.kbv.healthcheck.exceptions;

public class SOAPException extends RuntimeException {
    public SOAPException(String message, Exception exception) {
        super(message, exception);
    }
}
