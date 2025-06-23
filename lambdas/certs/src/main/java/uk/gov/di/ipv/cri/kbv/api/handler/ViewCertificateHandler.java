package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ViewCertificateHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ExperianSecrets experianSecrets;

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

    public ViewCertificateHandler() {
        this.experianSecrets = new ExperianSecrets();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            byte[] keystore = Base64.getDecoder().decode(experianSecrets.getKeystoreSecret());
            char[] password = experianSecrets.getKeystorePassword().toCharArray();

            CustomTrustManager.bind(keystore, password);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind CustomTrustManager: " + e);
        }

        try {
            HttpResponse<String> experianResponse = sendRequestUsingDefault();
            String body =
                    """
                    {
                        "status": "%s",
                        "body": "%s"
                    }
                    """
                            .formatted(experianResponse.statusCode(), experianResponse.body());

            response.setBody(body);
            response.setStatusCode(200);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send request to experian: " + e);
        }

        return response;
    }

    private HttpResponse<String> sendRequestUsingDefault() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(Configuration.WASP_HOST))
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
