package uk.gov.di.ipv.cri.kbv.api.security;

import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class Base64TokenEncoder {
    private final String key;
    private String token;

    public Base64TokenEncoder(String key, SoapToken soapToken) {
        this.key = Objects.requireNonNull(key, "The key must not be null");
        this.token = Objects.requireNonNull(soapToken.getToken(), "The token must not be null");
    }

    public String getToken() {
        if (token.contains("Error")) {
            throw new InvalidSoapTokenException("The SOAP token contains an error: " + token);
        }
        try {
            return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new InvalidSoapTokenException(
                    "Failed to encode the SOAP token: " + token + ": " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Key: " + key;
    }
}
