package uk.gov.di.ipv.cri.kbv.api.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.decodeTokenPayload;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.isTokenPayloadValid;

public class SoapTokenRetriever {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_NUMBER_OF_TOKEN_RETRIES = 3;
    private static final long DELAY_BETWEEN_RETRY_MS = 500;
    private static final long TOKEN_EXPIRY_THRESHOLD = TimeUnit.HOURS.toSeconds(2);

    private final SoapToken soapToken;
    private String cachedToken;

    public SoapTokenRetriever(SoapToken soapToken) {
        this.soapToken = soapToken;
        this.cachedToken = null;
    }

    public String getSoapToken() {
        if (isCachedTokenValidAndWithinThreshold()) {
            LOGGER.info("Using cached SOAP token");
            return cachedToken;
        }
        LOGGER.info("Retrieving SOAP token from Experian...");
        String token = null;

        for (int retry = 0; retry < MAX_NUMBER_OF_TOKEN_RETRIES; retry++) {
            sleepBeforeRetry(retry);
            try {
                token = soapToken.getToken();
                if (isTokenValidWithinThreshold(decodeTokenPayload(token))) {
                    LOGGER.info("Successfully retrieved a valid token.");
                    cachedToken = token;
                    return cachedToken;
                }
            } catch (NullPointerException e) {
                LOGGER.debug("Error while getting SOAP token: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error while getting SOAP token: {}", e.getMessage());
            }
        }

        String fallbackToken = cacheExperianTokenOutsideThreshold(token);

        if (fallbackToken != null) {
            return fallbackToken;
        }
        LOGGER.warn(
                "Received an invalid SOAP token from Experian after retries, using valid cached token for now...");
        return cachedToken;
    }

    private String cacheExperianTokenOutsideThreshold(String token) {
        if (token != null && !isCachedTokenValid()) {
            try {
                if (isTokenPayloadValid(decodeTokenPayload(token))) {
                    cachedToken = token;
                    LOGGER.info(
                            "Updated cached token with the one received from Experian. "
                                    + "The token given by Experian is valid but not "
                                    + "within our threshold, using anyway...");
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "Cached SOAP token and token from Experian token are both invalid: {}",
                        e.getMessage());
            }
            return token;
        }
        return null;
    }

    public boolean isCachedTokenValid() {
        try {
            return cachedToken != null && isTokenPayloadValid(decodeTokenPayload(cachedToken));
        } catch (Exception e) {
            LOGGER.debug("Token validation failed due to an error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isCachedTokenValidAndWithinThreshold() {
        try {
            return cachedToken != null
                    && isTokenValidWithinThreshold(decodeTokenPayload(cachedToken));
        } catch (Exception e) {
            LOGGER.debug("Threshold validation failed due to error: {}", e.getMessage());
            return false;
        }
    }

    public boolean hasTokenExpired() {
        try {
            return SoapTokenUtils.getTokenExpiry(
                            SoapTokenUtils.decodeTokenPayload(this.getSoapToken()))
                    < Instant.now().getEpochSecond();
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Token expiration check failed due to a JsonProcessingException error: {}",
                    e.getMessage());
            return true;
        } catch (Exception e) {
            LOGGER.debug("Token expiration check failed due to an error: {}", e.getMessage());
            return true;
        }
    }

    private static boolean isTokenValidWithinThreshold(String decodeTokenPayload)
            throws JsonProcessingException {
        long tokenExpiry = SoapTokenUtils.getTokenExpiry(decodeTokenPayload);
        return isTokenPayloadValid(decodeTokenPayload)
                && tokenExpiry > (Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD);
    }

    private static void sleepBeforeRetry(int currentIteration) {
        if (currentIteration > 0) {
            try {
                Thread.sleep(DELAY_BETWEEN_RETRY_MS * currentIteration);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
