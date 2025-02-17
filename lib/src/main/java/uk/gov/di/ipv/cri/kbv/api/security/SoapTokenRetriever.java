package uk.gov.di.ipv.cri.kbv.api.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.isTokenPayloadValid;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.isTokenValidWithinThreshold;

public class SoapTokenRetriever {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_NUMBER_OF_TOKEN_RETRIES = 3;
    private static final long DELAY_BETWEEN_RETRY_MS = 500;
    public static final String UPDATED_CACHED_TOKEN_WITH_RECEIVED_EXPERIAN_TOKEN =
            "Updated cached token with the one received from Experian. "
                    + "The token given by Experian is valid but not "
                    + "within our threshold, using anyway...";
    public static final String EXPERIAN_TOKEN_THAT_IS_OUTSIDE_OUR_THRESHOLD_AND_INVALID =
            "Returning an Experian token that is outside our threshold and invalid, using anyway...";
    public static final String CACHED_TOKEN_THAT_IS_OUTSIDE_SET_THRESHOLD =
            "Returning a cached token that is outside our threshold, using anyway...";
    private final SoapToken soapToken;
    private String cachedToken;

    public SoapTokenRetriever(SoapToken soapToken) {
        this.soapToken = soapToken;
        this.cachedToken = null;
    }

    public String getSoapToken() {
        if (cachedToken != null && isTokenValidWithinThreshold(cachedToken)) {
            LOGGER.info("Using cached SOAP token");
            return this.cachedToken;
        }

        LOGGER.info("Retrieving SOAP token from Experian...");
        String token = null;

        for (int retry = 0; retry < MAX_NUMBER_OF_TOKEN_RETRIES; retry++) {
            sleepBeforeRetry(retry);
            try {
                token = soapToken.getToken();
                if (token == null) {
                    LOGGER.warn("Received null token from Experian.");
                    continue;
                }
                if (isTokenValidWithinThreshold(token)) {
                    LOGGER.info("Successfully retrieved a valid token.");
                    this.cachedToken = token;
                    return this.cachedToken;
                }
            } catch (Exception e) {
                LOGGER.error("Error while getting SOAP token: {}", e.getMessage());
            }
        }

        if (isTokenPayloadValid(token)) {
            this.cachedToken = token;
            LOGGER.info(UPDATED_CACHED_TOKEN_WITH_RECEIVED_EXPERIAN_TOKEN);
        } else if (isTokenPayloadValid(cachedToken)) {
            LOGGER.info(CACHED_TOKEN_THAT_IS_OUTSIDE_SET_THRESHOLD);
            return this.cachedToken;
        } else if (token != null) {
            LOGGER.info(EXPERIAN_TOKEN_THAT_IS_OUTSIDE_OUR_THRESHOLD_AND_INVALID);
            this.cachedToken = token;
            return this.cachedToken;
        }
        return this.cachedToken;
    }

    public boolean hasTokenExpired(String tokenValue) {
        return SoapTokenUtils.hasTokenExpired(tokenValue);
    }

    private static void sleepBeforeRetry(int currentIteration) {
        if (currentIteration <= 0) return;

        try {
            Thread.sleep(DELAY_BETWEEN_RETRY_MS * currentIteration);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
