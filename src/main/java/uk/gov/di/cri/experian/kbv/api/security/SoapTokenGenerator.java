package uk.gov.di.cri.experian.kbv.api.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class SoapTokenGenerator {
    private final String token;

    public SoapTokenGenerator(String token) {
        this.token = token;
    }

    public String getToken() {
        Objects.requireNonNull(token, "The input must not be null");

        if (token.contains("Error")) {
            throw new RuntimeException();
        }

        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
