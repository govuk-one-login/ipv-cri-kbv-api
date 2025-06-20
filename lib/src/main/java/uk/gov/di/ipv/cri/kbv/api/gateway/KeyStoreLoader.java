package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.util.Base64;
import java.util.Objects;

public class KeyStoreLoader {
    private final ConfigurationService configurationService;

    public KeyStoreLoader(ConfigurationService configurationService) {
        this.configurationService =
                Objects.requireNonNull(
                        configurationService, "configurationService must not be null");
    }

    private char[] getPassword() {
        return this.configurationService.getSecretValue("experian/keystore-password").toCharArray();
    }

    private byte[] getKeyStore() {
        return Base64.getDecoder().decode(configurationService.getSecretValue("experian/keystore"));
    }

    public void load() {
        CompositeTrustStore.loadCertificates(getKeyStore(), getPassword());
    }
}
