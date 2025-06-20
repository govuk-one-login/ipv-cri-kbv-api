package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.exception.TrustManagerException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

public class CompositeTrustStore implements X509TrustManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeTrustStore.class);
    private final X509TrustManager defaultTrustManager;
    private final X509TrustManager customTrustManager;

    private CompositeTrustStore(
            X509TrustManager defaultTrustManager, X509TrustManager customTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
        this.customTrustManager = customTrustManager;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            defaultTrustManager.checkClientTrusted(chain, authType);
        } catch (Exception e) {
            customTrustManager.checkClientTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (Exception e) {
            customTrustManager.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] defaultIssuers = defaultTrustManager.getAcceptedIssuers();
        X509Certificate[] customIssuers = customTrustManager.getAcceptedIssuers();
        X509Certificate[] all = new X509Certificate[defaultIssuers.length + customIssuers.length];
        System.arraycopy(defaultIssuers, 0, all, 0, defaultIssuers.length);
        System.arraycopy(customIssuers, 0, all, defaultIssuers.length, customIssuers.length);
        return all;
    }

    public static void init(Map<String, byte[]> certificates) throws TrustManagerException {
        TrustManagerFactory defaultTrustManager;
        KeyStore customTrustStore;
        CertificateFactory certificateFactory;

        try {
            defaultTrustManager =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTrustManager.init((KeyStore) null);

            customTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customTrustStore.load(null);

            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new TrustManagerException("Failed to init trust store", e);
        }

        for (Map.Entry<String, byte[]> entry : certificates.entrySet()) {
            String caName = entry.getKey().toLowerCase();
            byte[] certBytes = entry.getValue();

            LOGGER.info("Loading certificate {}", caName);

            ByteArrayInputStream certStream = new ByteArrayInputStream(certBytes);
            try {
                X509Certificate cert =
                        (X509Certificate) certificateFactory.generateCertificate(certStream);
                customTrustStore.setCertificateEntry(caName, cert);
            } catch (Exception e) {
                throw new TrustManagerException(
                        "Failed to import %s into custom trust store".formatted(caName), e);
            }
        }

        TrustManagerFactory customTrustManagerFactory;

        try {
            customTrustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            customTrustManagerFactory.init(customTrustStore);
        } catch (Exception e) {
            throw new TrustManagerException("Failed to init custom TrustManagerFactory", e);
        }

        X509TrustManager combinedTrustManagers =
                new CompositeTrustStore(
                        (X509TrustManager) defaultTrustManager.getTrustManagers()[0],
                        (X509TrustManager) customTrustManagerFactory.getTrustManagers()[0]);

        SSLContext sslContext;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {combinedTrustManagers}, null);
        } catch (Exception e) {
            throw new TrustManagerException("Failed to init SSLContext", e);
        }

        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }
}
