package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LambdaHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TARGET_PATH = "/etc/pki/ca-trust";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        File caTrust = new File(TARGET_PATH);
        String responseBody;

        if (!caTrust.exists()) {
            responseBody = "Path does not exist: " + TARGET_PATH;
        } else if (caTrust.isFile()) {
            try {
                responseBody = Files.readString(caTrust.toPath());
            } catch (IOException e) {
                responseBody = "Error reading file: " + e.getMessage();
            }
        } else if (caTrust.isDirectory()) {
            responseBody = Arrays.stream(caTrust.listFiles())
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining("\n"));
        } else {
            responseBody = "Unknown file type at path: " + TARGET_PATH;
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(responseBody);
    }
}
