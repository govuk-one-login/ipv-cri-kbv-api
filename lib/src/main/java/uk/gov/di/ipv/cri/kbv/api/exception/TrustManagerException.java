package uk.gov.di.ipv.cri.kbv.api.exception;

public class TrustManagerException extends RuntimeException {
    public TrustManagerException(String message, Exception cause) {
        super(message, cause);
    }
}
