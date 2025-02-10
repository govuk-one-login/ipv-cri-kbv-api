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

    public SoapTokenRetriever(SoapToken soapToken) {
        this.soapToken = soapToken;
    }

    public String getSoapToken() {
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
        return token;
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
