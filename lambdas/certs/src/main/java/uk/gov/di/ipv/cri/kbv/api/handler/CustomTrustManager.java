package uk.gov.di.ipv.cri.kbv.api.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CustomTrustManager implements X509TrustManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTrustManager.class);
    private final X509TrustManager defaultTrustManager;

    public CustomTrustManager(X509TrustManager defaultTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        logCertificateChain("client", chain);

        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        logCertificateChain("server", chain);

        defaultTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    private void logCertificateChain(String type, X509Certificate[] chain) {
        LOGGER.info("Entry logCertificateChain");

        if (chain == null || chain.length == 0) {
            LOGGER.info("No {} certificate chain provided.", type);
            return;
        }

        LOGGER.info("Validating {} certificate chain ({} certificate(s)):", type, chain.length);

        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            LOGGER.info(
                    "[{} [Subject={}] [Issuer={}}",
                    i,
                    cert.getSubjectX500Principal(),
                    cert.getIssuerX500Principal());
        }
    }

    public static void bind(byte[] keystore, char[] password) throws Exception {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init((KeyStore) null);

        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");

        try (ByteArrayInputStream keystoreStream = new ByteArrayInputStream(keystore)) {
            clientKeyStore.load(keystoreStream, password);
        }

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, password);

        X509TrustManager customTrustManager =
                new CustomTrustManager(
                        (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(), new TrustManager[] {customTrustManager}, null);

        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }
}
