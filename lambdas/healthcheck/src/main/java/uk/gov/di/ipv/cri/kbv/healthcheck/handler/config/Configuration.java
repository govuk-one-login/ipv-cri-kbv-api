package uk.gov.di.ipv.cri.kbv.healthcheck.handler.config;

public final class Configuration {
    public static final int WASP_PORT = 443;

    public static final String CLIENT_ID = System.getenv("ClientId");
    public static final String KEYSTORE_SECRET = System.getenv("KeyStoreSecret");
    public static final String KEYSTORE_PASSWORD = System.getenv("KeyStorePassword");

    private Configuration() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}
