package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.soap;

import uk.gov.di.ipv.cri.kbv.api.gateway.CompositeTrustStore;
import uk.gov.di.ipv.cri.kbv.api.util.SoapTokenUtils;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore.Keystore;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keytool.Keytool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
        CompositeTrustStore.loadCertificates(keystore);
    }

    @Override
    public Report run() {
        Report report = new Report();

        String pfx = "/tmp/%d.pfx".formatted(System.currentTimeMillis());
        Keytool.importCertificate(pfx, jksFileLocation, keystorePassword);

        try {
            HttpResponse<String> response = sendRequestUsingDefault();
            System.out.println(response.statusCode());
            System.out.println(response.body());
        } catch (Exception e) {
            return new FailReport(e);
        }

        return report;
    }

    public HttpResponse<String> sendRequestUsingDefault() throws Exception {
        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(waspUrl))
                    .header("SOAPAction", SOAP_ACTION)
                    .header("Content-Type", CONTENT_TYPE)
                    .method("POST", HttpRequest.BodyPublishers.ofByteArray(SOAP_ENVELOPE.getBytes(StandardCharsets.UTF_8)))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
