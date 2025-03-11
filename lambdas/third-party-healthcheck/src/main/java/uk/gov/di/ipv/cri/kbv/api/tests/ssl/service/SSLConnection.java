package uk.gov.di.ipv.cri.kbv.api.tests.ssl.service;

import uk.gov.di.ipv.cri.kbv.api.exceptions.SSLHandshakeException;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.report.SSLHandshakeTestReport;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.util.Objects;

public class SSLConnection {
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private SSLConnection() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static SSLHandshakeTestReport testSSLHandshake(String host, int port)
            throws SSLPeerUnverifiedException {
        validateInputs(host, port);

        SSLSocket sslSocket = null;
        try {
            sslSocket = createSSLSocket(host, port);
            configureSocket(sslSocket);
            performHandshake(sslSocket);
            return createReport(sslSocket);
        } catch (IOException e) {
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (IOException ex) {
                    throw new SSLHandshakeException("Failed to close SSL socket", ex);
                }
                return createReport(sslSocket);
            }
            throw new SSLHandshakeException("Failed to establish SSL socket", e);
        }
    }

    private static void validateInputs(String host, int port) {
        Objects.requireNonNull(host, "Host must not be null");
        if (host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host must not be empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
    }

    private static SSLSocket createSSLSocket(String host, int port) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        return (SSLSocket) factory.createSocket(host, port);
    }

    private static void configureSocket(SSLSocket socket) throws IOException {
        socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
    }

    private static void performHandshake(SSLSocket socket) throws IOException {
        socket.startHandshake();
    }

    private static SSLHandshakeTestReport createReport(SSLSocket socket)
            throws SSLPeerUnverifiedException {
        return new SSLHandshakeTestReport(socket.getSession());
    }
}
