package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomTrustManagerTest {

    private X509TrustManager mockDefaultTrustManager;
    private CustomTrustManager customTrustManager;

    @BeforeEach
    void setUp() {
        mockDefaultTrustManager = mock(X509TrustManager.class);
        customTrustManager = new CustomTrustManager(mockDefaultTrustManager);
    }

    @Test
    void shouldCacheClientCertificatesOnCheckClientTrusted() throws Exception {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[] {cert1};

        when(cert1.getSubjectX500Principal()).thenReturn(new X500Principal("CN=ClientSubject"));
        when(cert1.getIssuerX500Principal()).thenReturn(new X500Principal("CN=ClientIssuer"));

        customTrustManager.checkClientTrusted(chain, "RSA");

        verify(mockDefaultTrustManager).checkClientTrusted(chain, "RSA");

        List<Certificate> cachedClientCerts = customTrustManager.getClientCertificates();
        assertEquals(1, cachedClientCerts.size());
        assertEquals("CN=ClientSubject", cachedClientCerts.get(0).subject());
        assertEquals("CN=ClientIssuer", cachedClientCerts.get(0).issuer());
    }

    @Test
    void shouldCacheServerCertificatesOnCheckServerTrusted() throws Exception {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate[] chain = new X509Certificate[] {cert1};

        when(cert1.getSubjectX500Principal()).thenReturn(new X500Principal("CN=ServerSubject"));
        when(cert1.getIssuerX500Principal()).thenReturn(new X500Principal("CN=ServerIssuer"));

        customTrustManager.checkServerTrusted(chain, "RSA");

        verify(mockDefaultTrustManager).checkServerTrusted(chain, "RSA");

        List<Certificate> cachedServerCerts = customTrustManager.getServerCertificates();
        assertEquals(1, cachedServerCerts.size());
        assertEquals("CN=ServerSubject", cachedServerCerts.get(0).subject());
        assertEquals("CN=ServerIssuer", cachedServerCerts.get(0).issuer());
    }

    @Test
    void shouldReturnAcceptedIssuersFromDefaultTrustManager() {
        X509Certificate[] issuers = new X509Certificate[0];
        when(mockDefaultTrustManager.getAcceptedIssuers()).thenReturn(issuers);

        X509Certificate[] result = customTrustManager.getAcceptedIssuers();
        assertSame(issuers, result);
    }
}
