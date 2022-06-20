package uk.gov.di.ipv.cri.kbv.api.exception;

public class KbvItemNotFoundException extends RuntimeException {
    public KbvItemNotFoundException(String message) {
        super(message);
    }
}
