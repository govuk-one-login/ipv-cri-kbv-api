package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final String waspUrl;
    private final char[] keystorePassword;
    private final byte[] keystore;

    public SOAPRequestAssertion(String keystorePassword, String waspUrl, String keystore) {
        this.keystorePassword = keystorePassword.toCharArray();
        this.waspUrl = waspUrl;
        this.keystore = Base64.getDecoder().decode(keystore);
    }

    @Override
    public Report run() {
        Report report = new Report();

        try {
            CustomTrustManager customTrustManager =
                    CustomTrustManager.bind(keystore, keystorePassword);

            HttpResponse<String> response = sendRequest();

            AtomicBoolean success = new AtomicBoolean(false);
            report.addAttributes("soap_request", processResponse(success, response));
            report.addAttributes(
                    "trust_manager",
                    Map.of(
                            "server", customTrustManager.getServerCertificates(),
                            "client", customTrustManager.getClientCertificates()));

            report.setPassed(success.get());

        } catch (Exception e) { // NOSONAR
            return new FailReport(e); // NOSONAR
        }

        return report;
    }

    private static Map<String, Object> processResponse(
            AtomicBoolean success, HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();
        Map<String, List<String>> headers = response.headers().map();

        boolean valid = isSoapResponseBodyValid(body);
        success.set(valid);

        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("contentLength", body.length());
        result.put("headers", headers);
        result.put("soapTokenValid", valid);

        return result;
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

    private HttpResponse<String> sendRequest() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(waspUrl))
                            .header("SOAPAction", SOAP_ACTION)
                            .header("Content-Type", CONTENT_TYPE)
                            .method(
                                    "POST",
                                    HttpRequest.BodyPublishers.ofByteArray(
                                            SOAP_ENVELOPE.getBytes(StandardCharsets.UTF_8)))
                            .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
