package uk.gov.di.ipv.cri.kbv.healthcheck.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.config.ExperianSecrets;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String MOCK_WASP_URL =
            "https://domain.com/WASPAuthenticator/tokenService.asmx";
    private static final String MOCK_KEYSTORE_ALIAS = "dummyAlias";
    private static final String MOCK_KEYSTORE_PASSWORD = "dummyPassword";
    private static final String MOCK_KEYSTORE_SECRET;

    static {
        try {
            KeyStore keyStore =
                    createMockKeyStore(MOCK_KEYSTORE_ALIAS, MOCK_KEYSTORE_PASSWORD.toCharArray());
            byte[] keyStoreAsByteArray =
                    convertKeyStoreToByteArray(keyStore, MOCK_KEYSTORE_PASSWORD.toCharArray());
            MOCK_KEYSTORE_SECRET = Base64.getEncoder().encodeToString(keyStoreAsByteArray);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock keystore", e);
        }
    }

    private HealthCheckHandler handler;

    @BeforeEach
    void setup() {
        ExperianSecrets experianSecrets = mock(ExperianSecrets.class);

        when(experianSecrets.getKeystorePassword()).thenReturn(MOCK_KEYSTORE_PASSWORD);
        when(experianSecrets.getKeystoreSecret()).thenReturn(MOCK_KEYSTORE_SECRET);
        when(experianSecrets.getWaspUrl()).thenReturn(MOCK_WASP_URL);

        handler = new HealthCheckHandler(experianSecrets);
    }

    @Test
    void shouldSuccessfullyGenerateReports() throws JsonProcessingException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/info");
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mock(Context.class));

        JsonNode body = OBJECT_MAPPER.readTree(response.getBody());

        assertNotNull(body.get("SSLHandshakeAssertion"));
        assertNotNull(body.get("Overview"));
        assertNotNull(body.get("SOAPRequestAssertion"));
        assertNotNull(body.get("KeyToolAssertion"));
        assertNotNull(body.get("KeyStoreAssertion"));
        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 400);
    }

    @Test
    void sOAPRequestAssertionContainsTrustManager() throws JsonProcessingException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/info");
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mock(Context.class));

        JsonNode body = OBJECT_MAPPER.readTree(response.getBody());
        JsonNode soapReport = body.get("SOAPRequestAssertion");
        JsonNode attributes = soapReport.get("attributes");
        JsonNode trustManager = attributes.get("trust_manager");

        assertTrue(attributes.has("trust_manager"));
        assertTrue(trustManager.has("server"));
        assertTrue(trustManager.has("client"));
    }

    @Test
    void shouldReturnEmptyBody() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mock(Context.class));

        assertEquals("{}", response.getBody());
    }

    /******************
     * KeyStore Setup *
     ******************/

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        X500Name name = new X500Name("CN=Test Certificate");
        X509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(
                        name,
                        new BigInteger(64, new SecureRandom()),
                        new Date(now),
                        new Date(now + 365L * 24 * 60 * 60 * 1000),
                        name,
                        keyPair.getPublic());
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
        ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    public static KeyStore createMockKeyStore(String alias, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateSelfSignedCertificate(keyPair);
        X509Certificate[] chain = new X509Certificate[] {cert};
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, chain);
        return keyStore;
    }

    private static byte[] convertKeyStoreToByteArray(KeyStore keystore, char[] password)
            throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        keystore.store(byteArrayOutputStream, password);
        return byteArrayOutputStream.toByteArray();
    }
}
