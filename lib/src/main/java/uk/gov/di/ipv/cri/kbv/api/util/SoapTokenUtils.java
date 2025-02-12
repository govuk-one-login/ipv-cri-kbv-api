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
        String encodedPayload = token.split("\\.", 3)[1];
        return new String(Base64.getUrlDecoder().decode(encodedPayload));
    }

    public static long getTokenExpiry(String tokenPayload) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(tokenPayload).get("exp").asLong();
    }

    public static boolean isTokenValid(String tokenPayload) {
        try {
            return tokenPayload != null
                    && !tokenPayload.isEmpty()
                    && !tokenPayload.toLowerCase().contains("error")
                    && getTokenExpiry(tokenPayload) > Instant.now().getEpochSecond();
        } catch (Exception e) {
            return false;
        }
    }
}
