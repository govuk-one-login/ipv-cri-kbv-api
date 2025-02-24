package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class SoapTokenUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger();
    public static final long TOKEN_EXPIRY_THRESHOLD = TimeUnit.HOURS.toSeconds(2);
    public static final String TOKEN_EXPIRATION_CHECK_FAILED_JSON_PROCESSING_EXCEPTION =
            "Token expiration check failed due to a JsonProcessingException error: {}";

    public static final String FAILED_TO_EXTRACT_TOKEN_EXPIRY_JSON_PROCESSING_ERROR =
            "Failed to extract token expiry due to JSON processing error: {}";

    private SoapTokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getTokenExpiryAsDateTime(String tokenPayload) {
        try {
            LocalDateTime dateTime =
                    Instant.ofEpochSecond(getTokenExpiry(tokenPayload))
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse token expiry date: {}", e.getMessage());
            return "Invalid Token";
        }
    }

    public static long getTokenExpiry(String tokenPayload) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(decodeTokenPayload(tokenPayload)).get("exp").asLong();
    }

    public static boolean isTokenValidWithinThreshold(String tokenPayload) {
        try {
            return isTokenPayloadValid(tokenPayload)
                    && getTokenExpiry(tokenPayload)
                            > Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD;
        } catch (JsonProcessingException e) {
            LOGGER.debug(FAILED_TO_EXTRACT_TOKEN_EXPIRY_JSON_PROCESSING_ERROR, e.getMessage());
            return false;
        }
    }

    public static boolean hasTokenExpired(String cachedToken) {
        try {
            return SoapTokenUtils.getTokenExpiry(cachedToken)
                    < Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD;
        } catch (JsonProcessingException e) {
            LOGGER.error(TOKEN_EXPIRATION_CHECK_FAILED_JSON_PROCESSING_EXCEPTION, e.getMessage());
            return true;
        } catch (Exception e) {
            LOGGER.debug("Token expiration check failed due to an error: {}", e.getMessage());
            return true;
        }
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

    private static String decodeTokenPayload(String token) {
        String encodedPayload = token.split("\\.", 3)[1];

        return new String(Base64.getUrlDecoder().decode(encodedPayload));
    }
}
