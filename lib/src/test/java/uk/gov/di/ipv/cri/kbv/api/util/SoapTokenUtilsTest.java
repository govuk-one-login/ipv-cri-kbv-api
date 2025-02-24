package uk.gov.di.ipv.cri.kbv.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils.TOKEN_EXPIRY_THRESHOLD;

@ExtendWith(MockitoExtension.class)
public class SoapTokenUtilsTest {
    @Nested
    class TokenExpiry {
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
    }

    @Nested
    class TokenExpiryAsDateTime {
        @Test
        void returnsTokenExpiryAsDateTimeFromZeroEpoch() {
            String tokenPayload = generateToken("dummy");
            String actualDateTime = SoapTokenUtils.getTokenExpiryAsDateTime(tokenPayload);

            assertEquals("Invalid Token", actualDateTime);
        }

        @ParameterizedTest
        @CsvSource({"1700000000, '2023-11-14 22:13:20'", "32503680000, '3000-01-01 00:00:00'"})
        void convertEpochsToDateTime(long epochSeconds, String expectedDateTime) {
            String tokenPayload = generateToken("{\"exp\": " + epochSeconds + "}");

            String actualDateTime = SoapTokenUtils.getTokenExpiryAsDateTime(tokenPayload);

            assertEquals(expectedDateTime, actualDateTime);
        }
    }

    @Nested
    class TokenHasExpiry {
        @Test
        void hasTokenExpiredReturnsTrueWhenThereIsNoToken() {
            assertTrue(SoapTokenUtils.hasTokenExpired(null));
        }

        @Test
        void hasTokenExpiredReturnsTrueWhenTokenIsMalformed() {
            assertTrue(SoapTokenUtils.hasTokenExpired(generateToken("dummy")));
        }

        @Test
        void hasTokenExpiredReturnsTrueWhenTokenHasExpired() {
            String payload =
                    generateToken(
                            String.format(
                                    "{\"exp\": %d}",
                                    Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(-2)));

            assertTrue(SoapTokenUtils.hasTokenExpired(payload));
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
    }

    @Nested
    class TokenValidWithinThresHold {
        @Test
        void tokenIsValidWithinThresHold() {
            long futureExpiry = Instant.now().getEpochSecond() + TOKEN_EXPIRY_THRESHOLD + 10;

            String aValidToken = generateToken("{\"exp\": " + futureExpiry + "}");

            assertTrue(SoapTokenUtils.isTokenValidWithinThreshold(aValidToken));
        }

        @Test
        void expiredTokenIsNotValidWithThresHold() {
            long pastExpiry = Instant.now().getEpochSecond() - 10;

            String anExpiredToken = generateToken("{\"exp\": " + pastExpiry + "}");

            assertFalse(SoapTokenUtils.isTokenValidWithinThreshold(anExpiredToken));
        }

        @Test
        void malFormedTokenIsNotValidWithThresHold() {
            String aMalformedToken = generateToken("dummy");

            assertFalse(SoapTokenUtils.isTokenValidWithinThreshold(aMalformedToken));
        }
    }

    @Nested
    class TokenPayloadValid {
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
