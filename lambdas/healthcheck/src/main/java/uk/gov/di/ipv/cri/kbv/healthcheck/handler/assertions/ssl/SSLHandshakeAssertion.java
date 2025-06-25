package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.ssl;

import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.EpochConverter;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.StringMasking;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public class SSLHandshakeAssertion implements Assertion {
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;

    public SSLHandshakeAssertion(String host, int port) {
        String domain = host.replace("https://", "");
        this.host = domain.substring(0, domain.indexOf("/"));
        this.port = port;
    }

    @Override
    public Report run() {
        var report = new Report();

        try (var sslSocket = createSSLSocket(host, port)) {
            configureSocket(sslSocket);
            performHandshake(sslSocket);

            SSLSession session = sslSocket.getSession();
            report.setPassed(session.isValid());

            report.addAttributes("session", getSessionInformation(session));
            report.addAttributes(
                    "serverCertificates",
                    Map.of(
                            "certificates",
                            getCertificateInformation(session.getPeerCertificates())));
        } catch (Exception e) {
            return new FailReport(e);
        }

        return report;
    }

    private static Map<String, Object> getSessionInformation(SSLSession session) {
        return Map.of(
                "protocol", session.getProtocol(),
                "peerHost", session.getPeerHost(),
                "peerPort", session.getPeerPort(),
                "sessionId",
                        StringMasking.maskString(
                                bytesToHex(session.getId()), session.getId().length / 2),
                "sessionValid", session.isValid(),
                "creationTime", EpochConverter.convertEpochMillisToDate(session.getCreationTime()));
    }

    private static List<Map<String, Object>> getCertificateInformation(Certificate[] certificates) {
        var certsInfo = new ArrayList<Map<String, Object>>();
        int index = 1;

        for (Certificate certificate : certificates) {
            if (certificate instanceof X509Certificate cert) {
                var certInfo = new HashMap<String, Object>();
                certInfo.put("certificateNumber", index++);
                certInfo.put("subject", cert.getSubjectX500Principal().getName());
                certInfo.put("issuer", cert.getIssuerX500Principal().getName());
                certInfo.put("serialNumber", cert.getSerialNumber().toString(16).toUpperCase());
                certInfo.put(
                        "validFrom", EpochConverter.convertEpochMillisToDate(cert.getNotBefore()));
                certInfo.put(
                        "validUntil", EpochConverter.convertEpochMillisToDate(cert.getNotAfter()));
                certInfo.put("version", "V%d".formatted(cert.getVersion()));
                certsInfo.add(certInfo);
            }
        }

        return certsInfo;
    }

    private static SSLSocket createSSLSocket(String host, int port) throws IOException {
        var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        return (SSLSocket) factory.createSocket(host, port);
    }

    private static void configureSocket(SSLSocket socket) throws IOException {
        socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
    }

    private static void performHandshake(SSLSocket socket) throws IOException {
        socket.startHandshake();
    }

    private static String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
