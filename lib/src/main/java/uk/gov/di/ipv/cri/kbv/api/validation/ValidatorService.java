package uk.gov.di.ipv.cri.kbv.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import uk.gov.di.ipv.cri.kbv.api.domain.SessionRequest;
import uk.gov.di.ipv.cri.kbv.api.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.kbv.api.exception.ValidationException;
import uk.gov.di.ipv.cri.kbv.api.service.ConfigurationService;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;

public class ValidatorService {

    public SessionRequest validateSessionRequest(String requestBody)
            throws ValidationException, ClientConfigurationException {
        SessionRequest sessionRequest = parseSessionRequest(requestBody);

        Map<String, String> clientAuthenticationConfig =
                getClientAuthenticationConfig(sessionRequest.getClientId());

        verifyRequestUri(sessionRequest, clientAuthenticationConfig);

        SignedJWT signedJWT = parseRequestJWT(sessionRequest);

        verifyJWTHeader(clientAuthenticationConfig, signedJWT);
        verifyJWTClaimsSet(clientAuthenticationConfig, signedJWT);
        verifyJWTSignature(clientAuthenticationConfig, signedJWT);

        return sessionRequest;
    }

    private SessionRequest parseSessionRequest(String requestBody) throws ValidationException {
        try {
            return new ObjectMapper().readValue(requestBody, SessionRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException("could not parse request body", e);
        }
    }

    private SignedJWT parseRequestJWT(SessionRequest sessionRequest) throws ValidationException {
        try {
            return SignedJWT.parse(sessionRequest.getRequestJWT());
        } catch (ParseException e) {
            throw new ValidationException("Could not parse request JWT", e);
        }
    }

    private Map<String, String> getClientAuthenticationConfig(String clientId)
            throws ValidationException {
        String path = String.format("clients/%s/jwtAuthentication", clientId);
        Map<String, String> clientConfig =
                ConfigurationService.getInstance().getParametersForPath(path);
        if (clientConfig == null || clientConfig.isEmpty()) {
            throw new ValidationException(
                    String.format("no configuration for client id '%s'", clientId));
        }
        return clientConfig;
    }

    private void verifyRequestUri(SessionRequest sessionRequest, Map<String, String> clientConfig)
            throws ValidationException {
        URI configRedirectUri = URI.create(clientConfig.get("redirectUri"));
        URI requestRedirectUri = sessionRequest.getRedirectUri();
        if (requestRedirectUri == null || !requestRedirectUri.equals(configRedirectUri)) {
            throw new ValidationException(
                    "redirect uri "
                            + requestRedirectUri
                            + " does not match configuration uri "
                            + configRedirectUri);
        }
    }

    private void verifyJWTHeader(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws ValidationException {
        JWSAlgorithm configuredAlgorithm =
                JWSAlgorithm.parse(clientAuthenticationConfig.get("authenticationAlg"));
        JWSAlgorithm jwtAlgorithm = signedJWT.getHeader().getAlgorithm();
        if (jwtAlgorithm != configuredAlgorithm) {
            throw new ValidationException(
                    String.format(
                            "jwt signing algorithm %s does not match signing algorithm configured for client: %s",
                            jwtAlgorithm, configuredAlgorithm));
        }
    }

    private void verifyJWTClaimsSet(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws ValidationException {
        DefaultJWTClaimsVerifier<?> verifier =
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(clientAuthenticationConfig.get("issuer"))
                                .build(),
                        new HashSet<>(Arrays.asList("exp", "nbf")));

        try {
            verifier.verify(signedJWT.getJWTClaimsSet(), null);
        } catch (BadJWTException | ParseException e) {
            throw new ValidationException("could not parse JWT", e);
        }
    }

    private void verifyJWTSignature(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws ValidationException, ClientConfigurationException {
        String publicCertificateToVerify =
                clientAuthenticationConfig.get("publicCertificateToVerify");
        try {
            PublicKey pubicKeyFromConfig = getPubicKeyFromConfig(publicCertificateToVerify);
            if (!validSignature(signedJWT, pubicKeyFromConfig)) {
                throw new ValidationException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new ValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ClientConfigurationException(e);
        }
    }

    private PublicKey getPubicKeyFromConfig(String base64) throws CertificateException {
        byte[] binaryCertificate = Base64.getDecoder().decode(base64);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate certificate =
                factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
        return certificate.getPublicKey();
    }

    private boolean validSignature(SignedJWT signedJWT, PublicKey clientPublicKey)
            throws JOSEException, ClientConfigurationException {
        if (clientPublicKey instanceof RSAPublicKey) {
            RSASSAVerifier rsassaVerifier = new RSASSAVerifier((RSAPublicKey) clientPublicKey);
            return signedJWT.verify(rsassaVerifier);
        } else if (clientPublicKey instanceof ECPublicKey) {
            ECDSAVerifier ecdsaVerifier = new ECDSAVerifier((ECPublicKey) clientPublicKey);
            return signedJWT.verify(ecdsaVerifier);
        } else {
            throw new ClientConfigurationException(
                    new IllegalStateException("unknown public JWT signing key"));
        }
    }
}
