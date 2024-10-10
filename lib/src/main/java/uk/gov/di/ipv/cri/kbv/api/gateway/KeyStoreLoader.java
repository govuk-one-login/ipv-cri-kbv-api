package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class KeyStoreLoader {
    private final ConfigurationService configurationService;

    public KeyStoreLoader(ConfigurationService configurationService) {
        this.configurationService =
                Objects.requireNonNull(
                        configurationService, "configurationService must not be null");
    }

    private String getKeyStorePath() throws IOException {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(configurationService.getSecretValue("experian/keystore")));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Persist keystore to file failed: " + e.getMessage());
        }
    }

    private String getPassword() {
        return this.configurationService.getSecretValue("experian/keystore-password");
    }

    public void load() throws IOException {
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStore", getKeyStorePath());
        System.setProperty("javax.net.ssl.keyStorePassword", getPassword());
    }
}
