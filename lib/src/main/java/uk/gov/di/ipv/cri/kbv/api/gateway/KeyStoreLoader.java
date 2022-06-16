package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

class KeyStoreLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreLoader.class);
    private final ConfigurationService configurationService;

    KeyStoreLoader(ConfigurationService configurationService) {
        this.configurationService =
                Objects.requireNonNull(
                        configurationService, "configurationService must not be null");
    }

    private String getKeyStorePath() {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(configurationService.getSecretValue("experian/keystore")));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException | IOException e) {
            LOGGER.error("Persist keystore to file failed", e);
            return null;
        }
    }

    private String getPassword() {
        return this.configurationService.getSecretValue("experian/keystore-password");
    }

    void load() {
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStore", getKeyStorePath());
        System.setProperty("javax.net.ssl.keyStorePassword", getPassword());
    }
}
