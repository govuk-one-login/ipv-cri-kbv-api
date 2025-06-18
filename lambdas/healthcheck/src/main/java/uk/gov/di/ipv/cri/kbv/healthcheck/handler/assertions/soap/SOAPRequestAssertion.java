package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;
import uk.gov.di.ipv.cri.kbv.healthcheck.exceptions.SOAPException;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore.Keystore;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keytool.Keytool;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SOAPRequestAssertion implements Assertion {
    private static final String SOAP_ENVELOPE =
            """
                    <?xml version="1.0" encoding="utf-8"?>
                    <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema" \
                    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                        <soap:Body>
                            <LoginWithCertificate xmlns="http://www.uk.experian.com/WASP/">
                                <application>GDS DI</application>
                                <checkIP>true</checkIP>
                            </LoginWithCertificate>
                        </soap:Body>
                    </soap:Envelope>
            """
                    .trim();

    private static final String SOAP_ACTION =
            "http://www.uk.experian.com/WASP/LoginWithCertificate";

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private final String keystorePassword;
    private final String waspUrl;
    private final String jksFileLocation;

    public SOAPRequestAssertion(String keystorePassword, String waspUrl, String keystore)
            throws IOException {
        this.keystorePassword = keystorePassword;
        this.waspUrl = waspUrl;
        this.jksFileLocation = Keystore.createKeyStoreFile(keystore);
    }

    @Override
    public Report run() {
        Report report = new Report();

        String pfx = "/tmp/%d.pfx".formatted(System.currentTimeMillis());
        Keytool.importCertificate(pfx, jksFileLocation, keystorePassword);

        try {
            SSLContext sslContext;

            if (waspUrl.contains("experian.com")) {
                sslContext = initializeSSLContextUsingCert(pfx, keystorePassword);
            } else {
                sslContext = initializeSSLContext();
            }

            HttpURLConnection connection = setupConnection(waspUrl, sslContext);
            sendRequest(connection);

            AtomicBoolean success = new AtomicBoolean(false);
            report.addAttributes("soap_request", processResponse(success, connection));
            report.setPassed(success.get());

            connection.disconnect();
        } catch (Exception e) {
            return new FailReport(e);
        }

        return report;
    }

    private static Map<String, Object> processResponse(
            AtomicBoolean success, HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        long date = connection.getDate();
        int contentLength = connection.getContentLength();

        Map<String, List<String>> headers =
                connection.getHeaderFields().entrySet().stream()
                        .filter(entry -> entry.getKey() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        boolean valid = isSoapResponseBodyValid(readResponse(connection));
        success.set(valid);

        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("date", date);
        result.put("contentLength", contentLength);
        result.put("headers", headers);
        result.put("soapTokenValid", valid);

        return result;
    }

    private static String readResponse(HttpURLConnection connection) {
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSoapResponseBodyValid(String token) {
        if (token == null) {
            return false;
        }

        String key = "<LoginWithCertificateResult>";

        if (token.contains(key)) {
            int start = token.indexOf(key) + key.length();
            int end = token.indexOf("</LoginWithCertificateResult>");
            return SoapTokenUtils.isTokenPayloadValid(token.substring(start, end));
        }

        return false;
    }

    private static void sendRequest(HttpURLConnection connection) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(SOAP_ENVELOPE.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private static SSLContext initializeSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);

            return sslContext;
        } catch (Exception e) {
            throw new SOAPException("Failed to initialize SSL context", e);
        }
    }

    private static SSLContext initializeSSLContextUsingCert(String pfxFile, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(pfxFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            KeyStore trustStore = KeyStore.getInstance("PKCS12");

            try (FileInputStream fis = new FileInputStream(pfxFile)) {
                trustStore.load(fis, password.toCharArray());
            }

            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext;
        } catch (Exception e) {
            throw new SOAPException("Failed to initialize SSL context", e);
        }
    }

    private static HttpURLConnection setupConnection(String url, SSLContext sslContext)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        if (connection instanceof HttpsURLConnection httpsURLConnection) {
            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        connection.setRequestProperty("SOAPAction", SOAP_ACTION);
        return connection;
    }
}
