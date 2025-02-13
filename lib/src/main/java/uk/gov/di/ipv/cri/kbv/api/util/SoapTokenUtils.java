package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Base64;

public class SoapTokenUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger();

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

    public static boolean isTokenPayloadValid(String tokenPayload) {
        try {
            return tokenPayload != null
                    && !tokenPayload.isEmpty()
                    && !tokenPayload.toLowerCase().contains("error")
                    && getTokenExpiry(tokenPayload) > Instant.now().getEpochSecond();
        } catch (Exception e) {
            LOGGER.debug("Token Payload validation failed due to an error: {}", e.getMessage());
            return false;
        }
    }
}
