package uk.gov.di.ipv.cri.kbv.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Base64TokenEncoderTest {

    private Base64TokenEncoder base64TokenEncoder;
    private SoapToken token = mock(SoapToken.class);

    @Test
    void shouldReturnAGeneratedBase64EncodedTokenWhenGivenASoapTokenService() {
        when(token.getToken()).thenReturn("token");
        base64TokenEncoder = new Base64TokenEncoder("token", token);

        assertEquals(
                Base64.getEncoder().encodeToString("token".getBytes(StandardCharsets.UTF_8)),
                base64TokenEncoder.getToken());
    }

    @Test
    void shouldThrowAnExceptionWhenGivenSoapTokenServiceIsNotValid() {
        base64TokenEncoder = new Base64TokenEncoder(null, token);
        when(token.getToken()).thenReturn(null);

        NullPointerException soapTokenNullRefException =
                assertThrows(NullPointerException.class, () -> base64TokenEncoder.getToken());

        assertEquals("The token must not be null", soapTokenNullRefException.getMessage());
    }

    @Test
    void shouldThrowAnRuntimeExceptionWhenGivenSoapTokenServiceIsNotValid() {
        base64TokenEncoder = new Base64TokenEncoder("Error", token);
        when(token.getToken()).thenReturn("Error");

        assertThrows(RuntimeException.class, () -> base64TokenEncoder.getToken());
    }
}
