package uk.gov.di.ipv.cri.kbv.api.utils;

import uk.gov.di.ipv.cri.kbv.api.exceptions.SOAPException;
import uk.gov.di.ipv.cri.kbv.api.models.LoginWithCertificateReport;

import javax.net.ssl.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LoginWithCertificate {
    private static final String SOAP_ENVELOPE =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                    + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "    <soap:Body>\n"
                    + "        <LoginWithCertificate xmlns=\"http://www.uk.experian.com/WASP/\">\n"
                    + "            <application>GDS - DI</application>\n"
                    + "            <checkIP>true</checkIP>\n"
                    + "        </LoginWithCertificate>\n"
                    + "    </soap:Body>\n"
                    + "</soap:Envelope>";

    private static final String SOAP_ACTION =
            "http://www.uk.experian.com/WASP/LoginWithCertificate";
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private LoginWithCertificate() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static LoginWithCertificateReport performRequest(
            String pfxFile, String password, String url) {
        validateInputs(pfxFile, password, url);

        try {
            SSLContext sslContext = initializeSSLContext(pfxFile, password);
            HttpURLConnection connection = setupConnection(url, sslContext);

            sendRequest(connection);
            LoginWithCertificateReport report = processResponse(connection);

            connection.disconnect();
            return report;

        } catch (Exception e) {
            throw new SOAPException("Failed to perform certificate login request", e);
        }
    }

    private static void validateInputs(String pfxFile, String password, String url) {
        Objects.requireNonNull(pfxFile, "PFX file path must not be null");
        Objects.requireNonNull(password, "Password must not be null");
        Objects.requireNonNull(url, "URL must not be null");

        if (pfxFile.trim().isEmpty() || password.trim().isEmpty() || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Input parameters must not be empty");
        }
    }

    private static SSLContext initializeSSLContext(String pfxFile, String password)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(pfxFile)) {
            keyStore.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static HttpURLConnection setupConnection(String url, SSLContext sslContext)
            throws IOException {
        URL endpoint = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();

        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(sslContext.getSocketFactory());
        }

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setRequestProperty("SOAPAction", SOAP_ACTION);

        return conn;
    }

    private static void sendRequest(HttpURLConnection conn) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = SOAP_ENVELOPE.getBytes(StandardCharsets.UTF_8);
            os.write(input);
            os.flush();
        }
    }

    private static LoginWithCertificateReport processResponse(HttpURLConnection conn)
            throws IOException {
        int statusCode = conn.getResponseCode();
        long date = conn.getDate();
        int contentLength = conn.getContentLength();

        Map<String, List<String>> headers =
                conn.getHeaderFields().entrySet().stream()
                        .filter(entry -> entry.getKey() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        LoginWithCertificateReport report = new LoginWithCertificateReport();
        report.setStatusCode(statusCode);
        report.setDate(date);
        report.setContentLength(contentLength);
        report.setHeaders(headers);
        report.setSoapTokenValid(isSoapResponseBodyValid(readResponse(conn)));

        return report;
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
        if (token.contains("LoginWithCertificateResult")) {
            int start =
                    token.indexOf("LoginWithCertificateResult")
                            + "LoginWithCertificateResult".length();
            int end = token.indexOf("</LoginWithCertificateResponse>");
            return SoapTokenUtils.isTokenPayloadValid(token.substring(start, end));
        }
        return false;
    }
}
