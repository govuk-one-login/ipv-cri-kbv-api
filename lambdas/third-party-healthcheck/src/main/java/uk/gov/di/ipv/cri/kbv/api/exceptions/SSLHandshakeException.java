package uk.gov.di.ipv.cri.kbv.api.exceptions;

public class SSLHandshakeException extends RuntimeException {
    public SSLHandshakeException(String message, Exception exception) {
        super(message, exception);
    }
}
