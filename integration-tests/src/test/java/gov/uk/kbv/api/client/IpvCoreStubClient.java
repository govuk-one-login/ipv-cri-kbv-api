package gov.uk.kbv.api.client;

import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IpvCoreStubClient {
    private static final String KBV_CRI_DEV = "kbv-cri-dev";
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;

    public IpvCoreStubClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;

        this.httpClient =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                clientConfigurationService.getIpvCoreStubUsername(),
                                                clientConfigurationService
                                                        .getIpvCoreStubPassword()
                                                        .toCharArray());
                                    }
                                })
                        .build();
    }

    public String getClaimsForUser(int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                                        .setPath("backend/generateInitialClaimsSet")
                                        .addParameter("cri", KBV_CRI_DEV)
                                        .addParameter(
                                                "rowNumber", String.valueOf(userDataRowNumber))
                                        .build())
                        .GET()
                        .build();
        return sendHttpRequest(request).body();
    }

    public String createSessionRequest(String requestBody)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", KBV_CRI_DEV)
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return sendHttpRequest(request).body();
    }

    public String getPrivateKeyJWTFormParamsForAuthCode(String authorizationCode)
            throws URISyntaxException, IOException, InterruptedException {
        var url =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("backend/createTokenRequestPrivateKeyJWT")
                        .addParameter("cri", KBV_CRI_DEV)
                        .addParameter("authorization_code", authorizationCode)
                        .build();

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        return sendHttpRequest(request).body();
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
