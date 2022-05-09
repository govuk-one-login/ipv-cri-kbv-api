package uk.gov.di.ipv.cri.experian.kbv.api.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class Base64TokenEncoder {
    private final String key;
    private String token;

    public Base64TokenEncoder(String key, SoapToken soapToken) {
        this.key = key;
        this.token = soapToken.getToken();
    }

    public String getToken() {
        Objects.requireNonNull(token, "The token must not be null");

        if (token.contains("Error")) {
            throw new RuntimeException();
        }

        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "Key: " + key;
    }
}
