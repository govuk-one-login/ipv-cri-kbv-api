package uk.gov.di.ipv.cri.kbv.api.service;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class OtelHttpConduit extends HTTPConduit {
    private static final Logger LOGGER = LoggerFactory.getLogger(OtelHttpConduit.class);

    private final OtelHttpClientWrapper otelHttpClientWrapper;
    private final HttpClient httpClient;

    public OtelHttpConduit(
            Bus bus,
            EndpointInfo endpointInfo,
            EndpointReferenceType target,
            OtelHttpClientWrapper otelHttpClientWrapper)
            throws IOException {
        super(bus, endpointInfo, target);
        this.otelHttpClientWrapper = otelHttpClientWrapper;
        this.httpClient = otelHttpClientWrapper.getTelemetryHttpClient();
    }

    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy)
            throws IOException {
        LOGGER.info("Setting up connection for URL: {}", address.getString());
        if (csPolicy != null) {
            LOGGER.info("Applying HTTP client policy settings...");
        }
    }

    @Override
    protected OutputStream createOutputStream(
            Message message, boolean needToCacheRequest, boolean isChunking, int chunkThreshold)
            throws IOException {
        LOGGER.info("Creating output stream for HTTP request.");
        return new ByteArrayOutputStream();
    }

    @Override
    public void close() {
        LOGGER.info("Closing OtelHttpConduit.");
        super.close();
    }

    /** Sends an HTTP request asynchronously using the OpenTelemetry instrumented HttpClient. */
    public CompletableFuture<HttpResponse<String>> sendRequestAsync(String url) {
        try {
            URI uri = URI.create(url);
            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

            LOGGER.info("Sending HTTP request to {}", url);
            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(
                            response -> {
                                LOGGER.info(
                                        "Received response with status: {}", response.statusCode());
                                return response;
                            });
        } catch (Exception e) {
            LOGGER.error("Error sending HTTP request", e);
            CompletableFuture<HttpResponse<String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}
