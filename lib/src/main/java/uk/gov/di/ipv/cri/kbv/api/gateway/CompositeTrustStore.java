package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class CompositeTrustStore implements X509TrustManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeTrustStore.class);
    private final X509TrustManager defaultTrustManager;
    private final X509TrustManager customTrustManager;

    public CompositeTrustStore(
            X509TrustManager defaultTrustManager, X509TrustManager customTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
        this.customTrustManager = customTrustManager;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

        logCertificateChain("client", chain);

        try {
            LOGGER.info("Attempting to validate client certificate using custom trust manager.");
            customTrustManager.checkClientTrusted(chain, authType);
            LOGGER.info("Client certificate validated successfully using custom trust manager.");
        } catch (CertificateException e) {
            LOGGER.warn(
                    "Default trust manager failed to validate client certificate: {}",
                    e.getMessage());
            LOGGER.info("Attempting to validate client certificate using default trust manager.");
            defaultTrustManager.checkClientTrusted(chain, authType);

            LOGGER.info("Client certificate validated successfully using default trust manager.");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {

        logCertificateChain("server", chain);

        try {
            LOGGER.info("Attempting to validate server certificate using custom trust manager.");
            customTrustManager.checkServerTrusted(chain, authType);
            LOGGER.info("Server certificate validated successfully using custom trust manager.");
        } catch (CertificateException e) {
            LOGGER.info(
                    "Default trust manager failed to validate server certificate: {}",
                    e.getMessage());
            LOGGER.info("Attempting to validate server certificate using default trust manager.");
            defaultTrustManager.checkServerTrusted(chain, authType);
            LOGGER.info("Server certificate validated successfully using default trust manager.");
        }
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
            LOGGER.info("[{} [Subject={}] [Issuer={}}", i, cert.getSubjectX500Principal(), cert.getIssuerX500Principal());
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

    public static void loadCertificates(String keystore) {
        Map<String, byte[]> certs =
                Map.ofEntries(
                        Map.entry(
                                "Sectigo Public Server Authentication Root R46",
                                readCert("4256644734.crt")),
                        Map.entry(
                                "Sectigo Public Server Authentication CA EV R36",
                                readCert("4267304687.crt")),
                        Map.entry(
                                "Sectigo Public Server Authentication CA OV R36",
                                readCert("4267304698.crt")));

        CompositeTrustStore.init(certs, keystore);
    }

    private static void init(Map<String, byte[]> certificates, String keystore)  {
        TrustManagerFactory defaultTrustManager;
        KeyStore customTrustStore;
        CertificateFactory certificateFactory;

        KeyStore clientKeyStore;

        byte[] decodedKeystore = Base64.getDecoder().decode(keystore);

        try {
            defaultTrustManager =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTrustManager.init((KeyStore) null);

            customTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customTrustStore.load(null);

            certificateFactory = CertificateFactory.getInstance("X.509");

            clientKeyStore = KeyStore.getInstance("PKCS12"); // or "JKS", depending on the input format
            try (ByteArrayInputStream keystoreStream = new ByteArrayInputStream(decodedKeystore)) {
                clientKeyStore.load(keystoreStream, "puppet".toCharArray());
            }


        } catch (Exception e) {
            throw new RuntimeException("Failed to init trust store", e);
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
                throw new RuntimeException(
                        "Failed to import %s into custom trust store".formatted(caName), e);
            }
        }

        TrustManagerFactory customTrustManagerFactory;

        try {
            customTrustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            customTrustManagerFactory.init(customTrustStore);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init custom TrustManagerFactory", e);
        }

        KeyManagerFactory keyManagerFactory;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, "puppet".toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KeyManagerFactory", e);
        }


        X509TrustManager combinedTrustManagers =
                new CompositeTrustStore(
                        (X509TrustManager) defaultTrustManager.getTrustManagers()[0],
                        (X509TrustManager) customTrustManagerFactory.getTrustManagers()[0]);

        SSLContext sslContext;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] {combinedTrustManagers}, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init SSLContext", e);
        }

        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

    }

    private static byte[] readCert(String path) {
        try (InputStream is =
                     KeyStoreLoader.class.getResourceAsStream("/certificates/%s".formatted(path))) {
            if (is == null) {
                throw new IllegalArgumentException("Certificate not found: " + path);
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read certificate: " + path, e);
        }
    }
}
