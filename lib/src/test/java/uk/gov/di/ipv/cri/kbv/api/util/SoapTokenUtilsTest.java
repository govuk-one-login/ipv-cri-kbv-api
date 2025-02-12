package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SoapTokenUtilsTest {

    @Test
    void shouldReturnTokenExpiry() throws JsonProcessingException {
        String payload = "{\"exp\":123456}";
        assertEquals(123456, SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldThrowWhenNoExpField() {
        String payload = "{\"foo\":123456}";
        assertThrows(Exception.class, () -> SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldThrowWhenMalformedJson() {
        String payload = "dummy";
        assertThrows(Exception.class, () -> SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldDefaultWhenNonIntExp() throws JsonProcessingException {
        String payload = "{\"exp\":\"foo\"}";
        assertEquals(0, SoapTokenUtils.getTokenExpiry(payload));
    }

    @Test
    void shouldDecodePayload() {
        String payload = "{\"foo\":\"bar\"}";
        String token = generateToken(payload);
        assertEquals(payload, SoapTokenUtils.decodeTokenPayload(token));
    }

    @Test
    void shouldNotValidateIfTokenIsError() {
        assertFalse(SoapTokenUtils.isTokenValid("error"));
    }

    @Test
    void shouldNotValidateIfTokenIsExpired() {
        String payload = "{\"exp\":\"0\"}";
        String token = generateToken(payload);
        assertFalse(SoapTokenUtils.isTokenValid(token));
    }

    @Test
    void shouldNotValidateIfTokenIsEmpty() {
        assertFalse(SoapTokenUtils.isTokenValid(""));
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
