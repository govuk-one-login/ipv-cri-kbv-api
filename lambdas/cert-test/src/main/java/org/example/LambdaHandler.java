package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LambdaHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        String url = "https://www.sectigo.com/";

        try {

            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest httpRequest =
                        HttpRequest.newBuilder()
                                .method("get", HttpRequest.BodyPublishers.noBody())
                                .uri(new URI(url))
                                .build();

                HttpResponse<String> res =
                        client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                String responseBody = "{ \"status\": \"%s\", }".formatted(res.statusCode());

                response.setStatusCode(200);
                response.setBody(responseBody);
            }

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody(e.getMessage());
        }
        return response;
    }
}
