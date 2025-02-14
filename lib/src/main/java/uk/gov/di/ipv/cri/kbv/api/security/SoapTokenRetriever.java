package uk.gov.di.ipv.cri.kbv.api.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
        if (cachedToken != null && isTokenValidWithinThreshold(cachedToken)) {
            LOGGER.info("Using cached SOAP token");

            return cachedToken;
        }
        LOGGER.info("Retrieving SOAP token from Experian...");
        String token = null;

        for (int retry = 0; retry < MAX_NUMBER_OF_TOKEN_RETRIES; retry++) {
            sleepBeforeRetry(retry);
            try {
                token = soapToken.getToken();
                if (isTokenValidWithinThreshold(token)) {
                    LOGGER.info("Successfully retrieved a valid token.");
                    this.cachedToken = token;
                    return cachedToken;
                }
            } catch (NullPointerException e) {
                LOGGER.debug("Error while getting SOAP token: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error while getting SOAP token: {}", e.getMessage());
            }
        }

        String fallbackToken = cacheExperianTokenOutsideThresholdIfPossible(token);

        if (fallbackToken != null) {
            return fallbackToken;
        }
        LOGGER.warn(
                "Received an invalid SOAP token from Experian after retries, using valid cached token for now...");
        return cachedToken;
    }

    private String cacheExperianTokenOutsideThresholdIfPossible(String token) {
        if (token == null || (cachedToken != null && isTokenPayloadValid(cachedToken))) {
            return null;
        }

        try {
            if (isTokenPayloadValid(token)) {
                this.cachedToken = token;
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

    public boolean hasTokenExpired(String cachedToken) {
        try {
            return SoapTokenUtils.getTokenExpiry(cachedToken) < Instant.now().getEpochSecond();
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

    private static boolean isTokenValidWithinThreshold(String tokenPayload) {
        try {
            long expiryTimestamp = SoapTokenUtils.getTokenExpiry(tokenPayload);
            long thresholdTimestamp = Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD;

            if (!isTokenPayloadValid(tokenPayload)) {
                LOGGER.debug("Token payload is invalid.");
                return false;
            }

            return expiryTimestamp > thresholdTimestamp;
        } catch (JsonProcessingException e) {
            LOGGER.debug(
                    "Failed to extract token expiry due to JSON processing error: {}",
                    e.getMessage());
            return false;
        }
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
