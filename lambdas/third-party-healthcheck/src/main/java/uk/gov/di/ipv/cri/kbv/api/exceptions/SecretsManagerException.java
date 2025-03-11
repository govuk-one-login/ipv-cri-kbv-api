package uk.gov.di.ipv.cri.kbv.api.exceptions;

public class SecretsManagerException extends RuntimeException {
    public SecretsManagerException(String message, Exception exception) {
        super(message, exception);
    }
}
