package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomX509TrustManagerTest {

    private X509TrustManager mockTrustManager;
    private CustomX509TrustManager customTrustManager;
    private X509Certificate mockCert;

    @BeforeEach
    void setUp() {
        mockTrustManager = mock(X509TrustManager.class);
        customTrustManager = new CustomX509TrustManager(mockTrustManager);

        mockCert = mock(X509Certificate.class);
        when(mockCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=TestSubject"));
        when(mockCert.getIssuerX500Principal()).thenReturn(new X500Principal("CN=TestIssuer"));
    }

    @Test
    void testCheckClientTrustedCachesCertificate() throws Exception {
        X509Certificate[] chain = new X509Certificate[] {mockCert};

        customTrustManager.checkClientTrusted(chain, "RSA");

        List<Certificate> cached = customTrustManager.getClientCertificates();

        verify(mockTrustManager).checkClientTrusted(chain, "RSA");
        assertEquals(1, cached.size());
        assertEquals("CN=TestSubject", cached.get(0).subject());
        assertEquals("CN=TestIssuer", cached.get(0).issuer());
    }

    @Test
    void testCheckServerTrustedCachesCertificate() throws Exception {
        X509Certificate[] chain = new X509Certificate[] {mockCert};

        customTrustManager.checkServerTrusted(chain, "RSA");

        List<Certificate> cached = customTrustManager.getServerCertificates();

        verify(mockTrustManager).checkServerTrusted(chain, "RSA");

        assertEquals(1, cached.size());
        assertEquals("CN=TestSubject", cached.get(0).subject());
        assertEquals("CN=TestIssuer", cached.get(0).issuer());
    }
}
