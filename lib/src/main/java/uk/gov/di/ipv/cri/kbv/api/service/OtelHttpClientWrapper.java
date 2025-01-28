package uk.gov.di.ipv.cri.kbv.api.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;

public class OtelHttpClientWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OtelHttpClientWrapper.class);
    private final HttpClient telemetryHttpClient;

    public OtelHttpClientWrapper() {
        this.telemetryHttpClient = createTelemetryHttpClient();
    }

    private HttpClient createTelemetryHttpClient() {
        LOGGER.info("Creating OpenTelemetry-instrumented HttpClient.");

        return JavaHttpClientTelemetry.builder(GlobalOpenTelemetry.get())
                .build()
                .newHttpClient(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .connectTimeout(Duration.ofSeconds(10))
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .build());
    }

    public HttpClient getTelemetryHttpClient() {
        return telemetryHttpClient;
    }
}
