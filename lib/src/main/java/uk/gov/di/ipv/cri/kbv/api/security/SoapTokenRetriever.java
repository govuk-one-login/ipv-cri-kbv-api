package uk.gov.di.ipv.cri.kbv.api.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.isTokenPayloadValid;

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

    private final Map<String, String> cachedTokens;

    public SoapTokenRetriever(SoapToken soapToken) {
        this.soapToken = soapToken;
        this.cachedTokens = new HashMap<>();
    }

    public String getSoapToken(String clientId) {
        if (cachedTokens.containsKey(clientId)
                && isTokenValidWithinThreshold(cachedTokens.get(clientId))) {
            LOGGER.info("Using cached SOAP token");
            return this.cachedTokens.get(clientId);
        }

        LOGGER.info("Retrieving SOAP token from Experian...");

        String token = attemptToFetchToken(clientId);

        if (isTokenPayloadValid(token)) {
            cachedTokens.put(clientId, token);
            LOGGER.info(UPDATED_CACHED_TOKEN_WITH_RECEIVED_EXPERIAN_TOKEN);
        } else if (isTokenPayloadValid(cachedTokens.get(clientId))) {
            LOGGER.info(CACHED_TOKEN_THAT_IS_OUTSIDE_SET_THRESHOLD);
            return cachedTokens.get(clientId);
        } else if (token != null) {
            LOGGER.info(EXPERIAN_TOKEN_THAT_IS_OUTSIDE_OUR_THRESHOLD_AND_INVALID);
            cachedTokens.put(clientId, token);
            return token;
        }
        return cachedTokens.get(clientId);
    }

    private String attemptToFetchToken(String clientId) {
        String token = null;

        for (int retry = 0; retry < MAX_NUMBER_OF_TOKEN_RETRIES; retry++) {
            sleepBeforeRetry(retry);
            try {
                token = soapToken.getToken(clientId);
                if (token == null) {
                    LOGGER.warn("Received null token from Experian.");
                    continue;
                }
                if (isTokenValidWithinThreshold(token)) {
                    LOGGER.info("Successfully retrieved a valid token.");

                    if (cachedTokens.containsKey(clientId)) {
                        cachedTokens.replace(clientId, token);
                    } else {
                        cachedTokens.put(clientId, token);
                    }

                    return token;
                }
            } catch (Exception e) {
                LOGGER.error("Error while getting SOAP token: {}", e.getMessage());
            }
        }

        return token;
    }

    public boolean isTokenValidWithinThreshold(String tokenValue) {
        return SoapTokenUtils.isTokenValidWithinThreshold(tokenValue);
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
