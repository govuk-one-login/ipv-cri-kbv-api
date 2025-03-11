package uk.gov.di.ipv.cri.kbv.api.tests.ssl.report;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SSLHandshakeTestReport {
    private String protocol;
    private String cipherSuite;
    private String peerHost;
    private int peerPort;
    private String sessionId;
    private boolean sessionValid;
    private Date creationTime;
    private Date lastAccessedTime;
    private List<CertificateDetails> serverCertificates;

    public SSLHandshakeTestReport() {
        // Empty constructor for Jackson
    }

    public SSLHandshakeTestReport(SSLSession session) throws SSLPeerUnverifiedException {
        this.protocol = session.getProtocol();
        this.cipherSuite = session.getCipherSuite();
        this.peerHost = session.getPeerHost();
        this.peerPort = session.getPeerPort();
        this.sessionId = bytesToHex(session.getId());
        this.sessionValid = session.isValid();
        this.creationTime = new Date(session.getCreationTime());
        this.lastAccessedTime = new Date(session.getLastAccessedTime());
        this.serverCertificates = new ArrayList<>();

        Certificate[] certificates = session.getPeerCertificates();

        for (int i = 0; i < certificates.length; i++) {
            if (certificates[i] instanceof X509Certificate) {
                this.serverCertificates.add(
                        new CertificateDetails((X509Certificate) certificates[i], i + 1));
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

    public void setCipherSuite(String cipherSuite) {
        this.cipherSuite = cipherSuite;
    }

    public String getPeerHost() {
        return peerHost;
    }

    public void setPeerHost(String peerHost) {
        this.peerHost = peerHost;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(int peerPort) {
        this.peerPort = peerPort;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSessionValid() {
        return sessionValid;
    }

    public void setSessionValid(boolean sessionValid) {
        this.sessionValid = sessionValid;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(Date lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public List<CertificateDetails> getServerCertificates() {
        return serverCertificates;
    }

    public void setServerCertificates(List<CertificateDetails> serverCertificates) {
        this.serverCertificates = serverCertificates;
    }
}
