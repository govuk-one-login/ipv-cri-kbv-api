package uk.gov.di.ipv.cri.kbv.api.exceptions;

public class SOAPException extends RuntimeException {
    public SOAPException(String message, Exception exception) {
        super(message, exception);
    }
}
