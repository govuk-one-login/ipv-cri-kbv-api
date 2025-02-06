package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;

public class SoapTokenUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SoapTokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String decodeTokenPayload(String token) {
        String[] jwtParts = token.split("\\.");
        String encodedPayload = jwtParts[1];
        String decodedToken = encodedPayload.replace('-', '+').replace('_', '/');
        int paddingLength = 4 - (decodedToken.length() % 4);
        if (paddingLength < 4) {
            decodedToken += "=".repeat(paddingLength);
        }
        return new String(Base64.getDecoder().decode(decodedToken));
    }

    public static long getTokenExpiry(String tokenPayload) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(tokenPayload).get("exp").asLong();
    }

    public static boolean isTokenValid(String tokenPayload) {
        try {
            return tokenPayload != null
                    && getTokenExpiry(tokenPayload) > Instant.now().getEpochSecond()
                    && !tokenPayload.trim().equalsIgnoreCase("error")
                    && !tokenPayload.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
