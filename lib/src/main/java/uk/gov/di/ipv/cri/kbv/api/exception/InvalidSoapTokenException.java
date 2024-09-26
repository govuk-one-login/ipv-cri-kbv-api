package uk.gov.di.ipv.cri.kbv.api.exception;

public class InvalidSoapTokenException extends RuntimeException {
    public InvalidSoapTokenException(String message) {
        super(message);
    }
}
