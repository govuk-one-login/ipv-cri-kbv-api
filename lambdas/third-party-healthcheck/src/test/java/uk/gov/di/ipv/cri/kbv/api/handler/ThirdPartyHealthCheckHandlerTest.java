package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyHealthCheckHandlerTest {
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

    @Mock private Context mockContext;
    @Mock private ServiceFactory mockServiceFactory;
    @Mock private SecretsProvider mockSecretsProvider;

    private final APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

    @BeforeAll
    static void setUp() throws Exception {
        setEnvironmentVariable("WaspURLSecret", "WaspURLSecret");
        setEnvironmentVariable("KeyStoreSecret", "KeyStoreSecret");
        setEnvironmentVariable("KeyStorePassword", "KeyStorePassword");
    }

    @BeforeEach
    void beforeEach() {
        event.setPath("/info");
        event.setBody("");

        String secretPrefix = "/null/";

        when(mockServiceFactory.getSecretsProvider()).thenReturn(mockSecretsProvider);

        when(mockSecretsProvider.get(secretPrefix + Configuration.KEYSTORE_PASSWORD))
                .thenReturn(MOCK_KEYSTORE_PASSWORD);

        when(mockSecretsProvider.get(secretPrefix + Configuration.KEYSTORE_SECRET))
                .thenReturn(MOCK_KEYSTORE_SECRET);

        when(mockSecretsProvider.get(secretPrefix + Configuration.WASP_URL_SECRET))
                .thenReturn(MOCK_WASP_URL);
    }

    @Test
    void shouldContainAllReportsInOutput() throws IOException {
        ThirdPartyHealthCheckHandler experianTestHandler =
                new ThirdPartyHealthCheckHandler(mockServiceFactory);

        APIGatewayProxyResponseEvent responseEvent =
                experianTestHandler.handleRequest(event, mockContext);

        JsonNode body = OBJECT_MAPPER.readTree(responseEvent.getBody());

        assertEquals(200, responseEvent.getStatusCode());
        assertNotNull(body.get("KeyStoreTest"));
        assertNotNull(body.get("AssertionTest"));
        assertNotNull(body.get("ImportCertificateTest"));
        assertNotNull(body.get("SOAPRequestTest"));
        assertNotNull(body.get("SSLHandshakeTest"));
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

    @SuppressWarnings("unchecked")
    public static void setEnvironmentVariable(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writableEnv = (Map<String, String>) field.get(env);
        writableEnv.put(key, value);
    }
}
