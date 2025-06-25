package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class CustomTrustManager implements X509TrustManager {
    private final X509TrustManager defaultTrustManager;
    private final List<Certificate> clientCertificates;
    private final List<Certificate> serverCertificates;

    public CustomTrustManager(X509TrustManager defaultTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
        this.clientCertificates = new ArrayList<>();
        this.serverCertificates = new ArrayList<>();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        cacheClientCertificate(chain);
        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        cacheServerCertificate(chain);
        defaultTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    private void cacheServerCertificate(X509Certificate[] chain) {
        for (X509Certificate cert : chain) {
            serverCertificates.add(toCertificate(cert));
        }
    }

    private void cacheClientCertificate(X509Certificate[] chain) {
        for (X509Certificate cert : chain) {
            clientCertificates.add(toCertificate(cert));
        }
    }

    private Certificate toCertificate(X509Certificate cert) {
        return new Certificate(
                cert.getSubjectX500Principal().getName(), cert.getIssuerX500Principal().getName());
    }

    public List<Certificate> getClientCertificates() {
        return clientCertificates;
    }

    public List<Certificate> getServerCertificates() {
        return serverCertificates;
    }

    public static CustomTrustManager bind(byte[] keystore, char[] password)
            throws NoSuchAlgorithmException,
                    IOException,
                    KeyManagementException,
                    KeyStoreException,
                    CertificateException,
                    UnrecoverableKeyException {
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

        CustomTrustManager customTrustManager =
                new CustomTrustManager(
                        (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(), new TrustManager[] {customTrustManager}, null);

        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        return customTrustManager;
    }
}
