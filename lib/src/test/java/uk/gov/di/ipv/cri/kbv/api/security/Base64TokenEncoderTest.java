package uk.gov.di.ipv.cri.kbv.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Base64TokenEncoderTest {

    private Base64TokenEncoder base64TokenEncoder;
    private final SoapToken token = mock(SoapToken.class);

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
        when(token.getToken()).thenReturn("Error");
        base64TokenEncoder = new Base64TokenEncoder("token", token);

        InvalidSoapTokenException soapTokenNullRefException =
                assertThrows(InvalidSoapTokenException.class, () -> base64TokenEncoder.getToken());

        assertEquals(
                "The SOAP token contains an error: Error", soapTokenNullRefException.getMessage());
    }

    @Test
    void shouldThrowAnRuntimeExceptionWhenGivenSoapTokenServiceIsNotValid() {
        when(token.getToken()).thenReturn("Error");
        base64TokenEncoder = new Base64TokenEncoder("Error", token);

        assertThrows(InvalidSoapTokenException.class, () -> base64TokenEncoder.getToken());
    }
}
