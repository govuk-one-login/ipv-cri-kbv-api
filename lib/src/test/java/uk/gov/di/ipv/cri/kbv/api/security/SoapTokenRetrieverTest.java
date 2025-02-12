package uk.gov.di.ipv.cri.kbv.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoapTokenRetrieverTest {
    private static final String MOCKED_VALID_TOKEN_VALUE = generateValidToken();
    private static final String MOCKED_EXPIRED_TOKEN_VALUE = generateExpiredToken();
    private static final String MOCKED_CLOSE_TO_EXPIRY_TOKEN = generateTokenExpiringIn5Mins();
    private static final String MOCKED_MALFORMED_TOKEN = generateMalformedToken();
    private static final String MOCKED_TOKEN_WITHOUT_EXP = generateTokenWithoutExpiryField();

    @Mock private SoapToken soapTokenMock;
    @InjectMocks private SoapTokenRetriever soapTokenRetriever;

    @Test
    void shouldReturnNullWhenTokenIsNull() {
        when(soapTokenMock.getToken()).thenReturn(null);

        String token = soapTokenRetriever.getSoapToken();

        assertNull(token);
    }

    @Test
    void shouldCacheToken() {
        when(soapTokenMock.getToken())
                .thenReturn(MOCKED_VALID_TOKEN_VALUE)
                .thenReturn(generateValidToken2());

        String goodToken = soapTokenRetriever.getSoapToken();
        String cachedGoodToken = soapTokenRetriever.getSoapToken();

        assertEquals(goodToken, cachedGoodToken);
    }

    @Test
    void shouldReturnCloseToExpiringCacheTokenWhenExperianGivesInvalidToken() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_CLOSE_TO_EXPIRY_TOKEN);
        String cachedGoodToken = soapTokenRetriever.getSoapToken();

        when(soapTokenMock.getToken()).thenReturn(MOCKED_EXPIRED_TOKEN_VALUE);

        String requestForNewToken = soapTokenRetriever.getSoapToken();

        assertEquals(cachedGoodToken, requestForNewToken);
    }

    @Test
    void shouldReturnInvalidToken() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_EXPIRED_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();
        String token2 = soapTokenRetriever.getSoapToken();

        assertEquals(MOCKED_EXPIRED_TOKEN_VALUE, token);
        assertEquals(MOCKED_EXPIRED_TOKEN_VALUE, token2);
    }

    @Test
    void shouldUpdateCache() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_CLOSE_TO_EXPIRY_TOKEN);

        String cachedCloseToExpiry = soapTokenRetriever.getSoapToken();

        when(soapTokenMock.getToken()).thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String updatedToken = soapTokenRetriever.getSoapToken();

        assertEquals(MOCKED_CLOSE_TO_EXPIRY_TOKEN, cachedCloseToExpiry);
        assertEquals(MOCKED_VALID_TOKEN_VALUE, updatedToken);
        assertNotEquals(cachedCloseToExpiry, updatedToken);
    }

    @Test
    void shouldReturnEmptyStringWhenTokenIsEmpty() {
        when(soapTokenMock.getToken()).thenReturn("");

        String token = soapTokenRetriever.getSoapToken();

        assertEquals("", token);
    }

    @Test
    void shouldReturnValidToken() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(1)).getToken();
        assertEquals(MOCKED_VALID_TOKEN_VALUE, token);
    }

    @Test
    void shouldReturnProvidedTokenWhenMalformed() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_MALFORMED_TOKEN);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_MALFORMED_TOKEN, token);
    }

    @Test
    void shouldReturnProvidedTokenWhenNoExpiryField() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_TOKEN_WITHOUT_EXP);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_TOKEN_WITHOUT_EXP, token);
    }

    @Test
    void shouldReturnValidTokenOnSecondRetry() {
        when(soapTokenMock.getToken())
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(2)).getToken();
        assertEquals(MOCKED_VALID_TOKEN_VALUE, token);
    }

    @Test
    void shouldReturnValidTokenOnThirdRetry() {
        when(soapTokenMock.getToken())
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_VALID_TOKEN_VALUE, token);
    }

    @Test
    void shouldReturnNotRetryMoreThanThreeTimes() {
        when(soapTokenMock.getToken())
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_EXPIRED_TOKEN_VALUE)
                .thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_EXPIRED_TOKEN_VALUE, token);
    }

    @Test
    void shouldRetryIfTokenIsExpiringSoon() {
        when(soapTokenMock.getToken())
                .thenReturn(MOCKED_CLOSE_TO_EXPIRY_TOKEN)
                .thenReturn(MOCKED_VALID_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(2)).getToken();
        assertEquals(MOCKED_VALID_TOKEN_VALUE, token);
    }

    @Test
    void shouldRetryOnError() {
        when(soapTokenMock.getToken()).thenReturn(MOCKED_EXPIRED_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_EXPIRED_TOKEN_VALUE, token);
    }

    @Test
    void shouldSleepBetweenRetries() {
        long expectedSleepDuration = 1000;
        long start = System.currentTimeMillis();

        when(soapTokenMock.getToken()).thenReturn(MOCKED_EXPIRED_TOKEN_VALUE);

        String token = soapTokenRetriever.getSoapToken();

        long end = System.currentTimeMillis();
        long expectedElapse = start + expectedSleepDuration;

        assertTrue(end >= expectedElapse);
        verify(soapTokenMock, times(3)).getToken();
        assertEquals(MOCKED_EXPIRED_TOKEN_VALUE, token);
    }

    private static final String TOKEN_HEADER =
            encodeBase64Url("{\"kid\": \"dummy-kid\", \"alg\": \"RS256\"}");
    private static final String TOKEN_SIGNATURE = encodeBase64Url("dummy-secret");

    public static String generateValidToken() {
        String encodedPayload =
                encodeBase64Url(
                        String.format(
                                "{\"exp\": \"%d\"}",
                                Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(1)));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    public static String generateValidToken2() {
        String encodedPayload =
                encodeBase64Url(
                        String.format(
                                "{\"exp\": %d}",
                                Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(2)));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    private static String generateExpiredToken() {
        String encodedPayload = encodeBase64Url(String.format("{\"exp\": \"%d\"}", 0));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    private static String generateTokenExpiringIn5Mins() {
        String encodedPayload =
                encodeBase64Url(
                        String.format(
                                "{\"exp\": \"%d\"}",
                                Instant.now().getEpochSecond() + TimeUnit.MINUTES.toSeconds(5)));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    private static String generateMalformedToken() {
        String encodedPayload =
                encodeBase64Url(
                        String.format(
                                "{\"exp\": \"%d\"",
                                Instant.now().getEpochSecond() + TimeUnit.MINUTES.toSeconds(5)));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    private static String generateTokenWithoutExpiryField() {
        String encodedPayload =
                encodeBase64Url(
                        String.format(
                                "{\"foo\": \"%d\"}",
                                Instant.now().getEpochSecond() + TimeUnit.MINUTES.toSeconds(5)));
        return TOKEN_HEADER + "." + encodedPayload + "." + TOKEN_SIGNATURE;
    }

    public static String encodeBase64Url(String input) {
        return Base64.getEncoder().withoutPadding().encodeToString(input.getBytes());
    }
}
