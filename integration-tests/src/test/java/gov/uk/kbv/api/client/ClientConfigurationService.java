package gov.uk.kbv.api.client;

import java.util.Objects;
import java.util.Optional;

public class ClientConfigurationService {
    private final String environment;
    private final String privateApiEndpoint;
    private final String publicApiEndpoint;
    private final String publicApiKey;
    private final String ipvCoreStubUrl;
    private final String ipvCoreStubUsername;
    private final String ipvCoreStubPassword;

    public ClientConfigurationService(String environment) {
        this.environment = environment;
        this.privateApiEndpoint =
                getApiEndpoint(
                        "API_GATEWAY_ID_PRIVATE",
                        "Environment variable API_GATEWAY_ID_PRIVATE is not set");
        this.publicApiEndpoint =
                getApiEndpoint(
                        "API_GATEWAY_ID_PUBLIC",
                        "Environment variable API_GATEWAY_ID_PUBLIC is not set");
        this.publicApiKey =
                Objects.requireNonNull(
                        System.getenv("APIGW_API_KEY"),
                        "Environment variable APIGW_API_KEY is not set");
        this.ipvCoreStubUrl =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_URL"),
                        "Environment variable IPV_CORE_STUB_URL is not set");
        this.ipvCoreStubUsername =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_USER is not set");
        this.ipvCoreStubPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_PASSWORD is not set");
    }

    String getPrivateApiEndpoint() {
        return this.privateApiEndpoint;
    }

    String getPublicApiEndpoint() {
        return this.publicApiEndpoint;
    }

    String getPublicApiKey() {
        return this.publicApiKey;
    }

    String getIPVCoreStubURL() {
        return this.ipvCoreStubUrl;
    }

    String getIpvCoreStubUsername() {
        return this.ipvCoreStubUsername;
    }

    String getIpvCoreStubPassword() {
        return this.ipvCoreStubPassword;
    }

    String createUriPath(String endpoint) {
        return String.format("/%s/%s", this.environment, endpoint);
    }

    private static String getApiEndpoint(String apikey, String message) {
        String restApiId =
                Optional.ofNullable(System.getenv(apikey))
                        .orElseThrow(() -> new IllegalArgumentException(message));

        return String.format("https://%s.execute-api.eu-west-2.amazonaws.com", restApiId);
    }
}
