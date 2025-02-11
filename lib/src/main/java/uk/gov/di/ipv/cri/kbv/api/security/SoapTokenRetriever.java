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

        int retry = 0;
        String token = null;
        boolean validTokenFetch = false;
        while (retry < MAX_NUMBER_OF_TOKEN_RETRIES && !validTokenFetch) {
            sleepIfRetry(retry);
            try {
                token = soapToken.getToken();
                validTokenFetch = isTokenValid(SoapTokenUtils.decodeTokenPayload(token));
            } catch (Exception e) {
                LOGGER.error("Error while getting soap token", e);
            }
            retry++;
        }

        if (!validTokenFetch && !isCachedTokenValid()) {
            try {
                if (token != null
                        && SoapTokenUtils.isTokenValid(SoapTokenUtils.decodeTokenPayload(token))) {
                    cachedToken = token;
                    LOGGER.info(
                            "Updated cached token with the one received from Experian. The token given by Experian is valid but not within our threshold, using anyway...");
                }
            } catch (Exception ignore) {
                LOGGER.warn("Cached SOAP token and token from Experian are both invalid");
            }

            return token;
        }

        if (validTokenFetch) {
            LOGGER.info("Cached token has been updated");
            cachedToken = token;
        } else {
            LOGGER.warn(
                    "Received an invalid SOAP token from Experian after retries, using valid cached token for now...");
        }

        return cachedToken;
    }

    public boolean isCachedTokenValid() {
        try {
            return cachedToken != null
                    && SoapTokenUtils.isTokenValid(SoapTokenUtils.decodeTokenPayload(cachedToken));
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isCachedTokenValidAndWithinThreshold() {
        try {
            return cachedToken != null
                    && isTokenValid(SoapTokenUtils.decodeTokenPayload(cachedToken));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isTokenValid(String decodeTokenPayload) throws JsonProcessingException {
        long tokenExpiry = SoapTokenUtils.getTokenExpiry(decodeTokenPayload);
        return SoapTokenUtils.isTokenValid(decodeTokenPayload)
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
