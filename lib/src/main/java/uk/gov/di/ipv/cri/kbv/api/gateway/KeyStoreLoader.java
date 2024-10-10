package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

public class KeyStoreLoader {
    private final ConfigurationService configurationService;

    public KeyStoreLoader(ConfigurationService configurationService) {
        this.configurationService =
                Objects.requireNonNull(
                        configurationService, "configurationService must not be null");
    }

    private String getKeyStorePath() throws IOException {
        Path tempFile = null;
        try {
            File file = File.createTempFile("prefix", "suffix", new File("."));
            FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rwx------"));
            Files.createTempFile("prefix", "suffix", attr);

            tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(configurationService.getSecretValue("experian/keystore")));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            deleteTempFile(tempFile);
            throw new IllegalStateException("Persist keystore to file failed: " + e.getMessage());
        }
    }

    private void deleteTempFile(Path tempFile) throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(Paths.get(tempFile.toString()));
        }
    }

    private String getPassword() {
        return this.configurationService.getSecretValue("experian/keystore-password");
    }

    public void load() throws IOException {
        String filePath = getKeyStorePath();
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStore", filePath);
        System.setProperty("javax.net.ssl.keyStorePassword", getPassword());
        deleteTempFile(Paths.get(filePath));
    }
}
