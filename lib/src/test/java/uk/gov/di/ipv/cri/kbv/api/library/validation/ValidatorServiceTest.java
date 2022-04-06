package uk.gov.di.ipv.cri.kbv.api.library.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.cri.kbv.api.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.library.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.library.service.SessionRequestBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatorServiceTest {

    private ValidatorService validatorService;

    MockedStatic<ConfigurationService> mockConfigurationSingletonService =
            Mockito.mockStatic(ConfigurationService.class);
    private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setup() {
        validatorService = new ValidatorService();
        String parameterPrefix = "parameterPrefix";
        SSMProvider mockSSMProvider = mock(SSMProvider.class);
        when(mockSSMProvider.recursive()).thenReturn(mockSSMProvider);
        when(mockSSMProvider.getMultiple(anyString())).thenReturn(Map.of("//", ""));
        mockConfigurationService = spy(new ConfigurationService(mockSSMProvider, parameterPrefix));
        mockConfigurationSingletonService
                .when(() -> ConfigurationService.getInstance())
                .thenReturn(mockConfigurationService);
    }

    @AfterEach
    void tearDown() {
        mockConfigurationSingletonService.close();
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestClientIdIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder().withClientId("bad-client-id");
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        when(mockConfigurationService.getParametersForPath(
                        "/clients/bad-client-id/jwtAuthentication"))
                .thenReturn(Map.of());
        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(exception.getMessage(), containsString("no configuration for client id"));
        verify(mockConfigurationService)
                .getParametersForPath("/clients/bad-client-id/jwtAuthentication");
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsInvalid() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        sessionRequest.setRequestJWT(
                Base64.getEncoder().encodeToString("not a jwt".getBytes(StandardCharsets.UTF_8)));

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(exception.getMessage(), containsString("Could not parse request JWT"));
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestUriIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder()
                        .withRedirectUri(URI.create("https://www.example.com/not-valid-callback"));
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));
        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri https://www.example.com/not-valid-callback does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldThrowValidationExceptionWhenClientX509CertDoesNotMatchPrivateKey() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setCertificateFile("wrong-cert.crt.pem");
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(exception.getMessage(), containsString("JWT signature verification failed"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTHeaderDoesNotMatchConfig() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setSigningAlgorithm(JWSAlgorithm.RS512);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(
                exception.getMessage(),
                containsString(
                        "jwt signing algorithm RS512 does not match signing algorithm configured for client: RS256"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsExpired() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setNow(Instant.now().minus(1, ChronoUnit.DAYS));
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                validatorService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(exception.getMessage(), containsString("could not parse JWT"));
    }

    @Test
    void shouldValidateJWTSignedWithRSAKey()
            throws IOException, ValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionRequest result =
                validatorService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    @Test
    void shouldValidateJWTSignedWithECKey()
            throws IOException, ValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        signedJWTBuilder.setPrivateKeyFile("signing_ec.pk8");
        signedJWTBuilder.setCertificateFile("signing_ec.crt.pem");
        signedJWTBuilder.setSigningAlgorithm(JWSAlgorithm.ES384);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        Map<String, String> configMap = standardSSMConfigMap(signedJWTBuilder);
        configMap.put("authenticationAlg", "ES384");
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(configMap);

        SessionRequest result =
                validatorService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    private String marshallToJSON(Object sessionRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(sessionRequest);
    }

    private Map<String, String> standardSSMConfigMap(
            SessionRequestBuilder.SignedJWTBuilder builder) {
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            map.put(
                    "publicCertificateToVerify",
                    Base64.getEncoder().encodeToString(builder.getCertificate().getEncoded()));
            return map;
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
