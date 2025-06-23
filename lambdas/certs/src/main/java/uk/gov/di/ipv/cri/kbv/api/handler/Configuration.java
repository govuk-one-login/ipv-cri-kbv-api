package uk.gov.di.ipv.cri.kbv.api.handler;

public final class Configuration {
    public static final String WASP_HOST =
            "https://secure.wasp.uat.uk.experian.com/WASPAuthenticator/tokenService.asmx";
    public static final String WASP_URL_SECRET = System.getenv("WaspURLSecret");
    public static final String KEYSTORE_SECRET = System.getenv("KeyStoreSecret");
    public static final String KEYSTORE_PASSWORD = System.getenv("KeyStorePassword");

    private Configuration() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}
