package uk.gov.di.ipv.cri.kbv.api.handler;

public class Configuration {
    public static final String JKS_FILE_LOCATION = "/tmp/experian.jks";
    public static final String PFX_FILE_LOCATION = "/tmp/experian.pfx";
    public static final String WASP_HOST = "secure.wasp.uk.experian.com";
    public static final int WASP_PORT = 443;

    public static final String WASP_URL_SECRET = System.getenv("WaspURLSecret");
    public static final String KEYSTORE_SECRET = System.getenv("KeyStoreSecret");
    public static final String KEYSTORE_PASSWORD = System.getenv("KeyStorePassword");

    private Configuration() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}
