package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SoapTokenUtilsTest {
    @Test
    void hasTokenExpiredReturnsTrueWhenTheirIsNoToken() {
        assertTrue(SoapTokenUtils.hasTokenExpired(null));
    }

    @Test
    void hasTokenExpiredReturnsFalseWhenTokenIsValid() {
        String payload =
                generateToken(
                        String.format(
                                "{\"exp\": %d}",
                                Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(2)));

        assertFalse(SoapTokenUtils.hasTokenExpired(payload));
    }

    @Test
    void shouldReturnTokenExpiry() throws JsonProcessingException {
        String payload = generateToken("{\"exp\":123456}");

        assertEquals(123456, SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldThrowWhenNoExpField() {
        String payload = generateToken("{\"foo\":123456}");
        assertThrows(Exception.class, () -> SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldThrowWhenMalformedJson() {
        String payload = generateToken("dummy");

        assertThrows(Exception.class, () -> SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldDefaultWhenNonIntExp() throws JsonProcessingException {
        String payload = generateToken("{\"exp\":\"foo\"}");

        assertEquals(0, SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldNotValidateIfTokenIsError() {
        assertFalse(SoapTokenUtils.isTokenPayloadValid("error"));
    }

    @Test
    void shouldNotValidateIfTokenIsExpired() {
        String payload = generateToken("{\"exp\":\"0\"}");
        String token = generateToken(payload);

        assertFalse(SoapTokenUtils.isTokenPayloadValid(token));
    }

    @Test
    void shouldNotValidateIfTokenIsEmpty() {

        assertFalse(SoapTokenUtils.isTokenPayloadValid(generateToken("")));
    }

    private static final String TOKEN_HEADER =
            encodeBase64Url("{\"kid\": \"dummy-kid\", \"alg\": \"RS256\"}");
    private static final String TOKEN_SIGNATURE = encodeBase64Url("dummy-secret");

    public static String generateToken(String payload) {
        return TOKEN_HEADER + "." + encodeBase64Url(payload) + "." + TOKEN_SIGNATURE;
    }

    public static String encodeBase64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes());
    }
}
