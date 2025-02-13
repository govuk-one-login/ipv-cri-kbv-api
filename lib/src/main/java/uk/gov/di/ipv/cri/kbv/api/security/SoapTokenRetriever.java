package uk.gov.di.ipv.cri.kbv.api.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
            sleepIfRetry(retry);
            try {
                token = soapToken.getToken();
                if (isTokenValidWithinThreshold(SoapTokenUtils.decodeTokenPayload(token))) {
                    LOGGER.info("Successfully retrieved a valid token.");
                    cachedToken = token;
                    return cachedToken;
                }
            } catch (Exception e) {
                LOGGER.error("Error while getting SOAP token: {}", e.getMessage());
            }
        }

        if (token != null && !isCachedTokenValid()) {
            try {
                if (SoapTokenUtils.isTokenPayloadValid(SoapTokenUtils.decodeTokenPayload(token))) {
                    cachedToken = token;
                    LOGGER.info(
                            "Updated cached token with the one received from Experian. The token given by Experian is valid but not within our threshold, using anyway...");
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "Cached SOAP token and retrieved token are both invalid: {}",
                        e.getMessage());
            }
            return token;
        }

        LOGGER.warn(
                "Received an invalid SOAP token from Experian after retries, using valid cached token for now...");
        return cachedToken;
    }

    public boolean isCachedTokenValid() {
        try {
            return cachedToken != null
                    && SoapTokenUtils.isTokenPayloadValid(
                            SoapTokenUtils.decodeTokenPayload(cachedToken));
        } catch (Exception e) {
            LOGGER.debug("Token validation failed due to an error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isCachedTokenValidAndWithinThreshold() {
        try {
            return cachedToken != null
                    && isTokenValidWithinThreshold(SoapTokenUtils.decodeTokenPayload(cachedToken));
        } catch (Exception e) {
            LOGGER.debug("Threshold validation failed due to error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isTokenValidWithinThreshold(String decodeTokenPayload)
            throws JsonProcessingException {
        long tokenExpiry = SoapTokenUtils.getTokenExpiry(decodeTokenPayload);
        return SoapTokenUtils.isTokenPayloadValid(decodeTokenPayload)
                && tokenExpiry > (Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD);
    }

    private static void sleepIfRetry(int currentIteration) {
        if (currentIteration > 0) {
            try {
                Thread.sleep(DELAY_BETWEEN_RETRY_MS * currentIteration);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
