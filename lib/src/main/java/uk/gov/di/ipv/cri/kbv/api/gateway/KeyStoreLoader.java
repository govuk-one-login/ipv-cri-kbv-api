package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

public class KeyStoreLoader {
    private final ConfigurationService configurationService;

    public KeyStoreLoader(ConfigurationService configurationService) {
        this.configurationService =
                Objects.requireNonNull(
                        configurationService, "configurationService must not be null");
    }

    private Path getKeyStorePath() throws IOException {
        Path tempFile = File.createTempFile("prefix", "suffix").toPath();
        try {
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(configurationService.getSecretValue("experian/keystore")));
            return tempFile;
        } catch (IllegalArgumentException | NullPointerException e) {
            deleteTempFile(tempFile);
            throw new IllegalStateException("Persist keystore to file failed: " + e.getMessage());
        }
    }

    public void load() throws IOException {
        Path filePath = getKeyStorePath();
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStore", filePath.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", getPassword());

        deleteTempFile(filePath);
    }

    private void deleteTempFile(Path tempFile) throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    private String getPassword() {
        return this.configurationService.getSecretValue("experian/keystore-password");
    }
}
