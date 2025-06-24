package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import javax.net.ssl.X509TrustManager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class CustomX509TrustManager implements X509TrustManager {
    private final X509TrustManager defaultTrustManager;
    private final List<Certificate> clientCertificates;
    private final List<Certificate> serverCertificates;

    public CustomX509TrustManager(X509TrustManager defaultTrustManager) {
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
}
