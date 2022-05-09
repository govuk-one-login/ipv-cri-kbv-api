package uk.gov.di.ipv.cri.experian.kbv.api.service;

public class KBVSystemProperty {
    public static final String JAVAX_NET_SSL_KEYSTORE = "javax.net.ssl.keyStore";
    public static final String JAVAX_NET_SSL_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
    public static final String JAVAX_NET_SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private final String value;
    private final String password;

    public KBVSystemProperty(KeyStoreService keyStoreService) {
        this.value = keyStoreService.getKeyStorePath();
        this.password = keyStoreService.getPassword();
    }

    public void save() {
        System.setProperty(JAVAX_NET_SSL_KEYSTORE, this.value);
        System.setProperty(JAVAX_NET_SSL_KEYSTORE_TYPE, "pkcs12");
        System.setProperty(JAVAX_NET_SSL_KEYSTORE_PASSWORD, this.password);
    }
}
