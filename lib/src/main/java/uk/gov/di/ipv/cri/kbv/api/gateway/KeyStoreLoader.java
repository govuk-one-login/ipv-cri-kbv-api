package uk.gov.di.ipv.cri.kbv.api.gateway;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.TrustManagerException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
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
            File file =
                    Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile(); // NOSONAR
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
        loadCertificates();

        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStore", getKeyStorePath());
        System.setProperty("javax.net.ssl.keyStorePassword", getPassword());
    }

    private static void loadCertificates() throws TrustManagerException {
        Map<String, byte[]> certs =
                Map.ofEntries(
                        Map.entry(
                                "Sectigo Public Server Authentication Root R46",
                                readCert("4256644734.crt")),
                        Map.entry(
                                "Sectigo Public Server Authentication CA EV R36",
                                readCert("4267304687.crt")),
                        Map.entry(
                                "Sectigo Public Server Authentication CA OV R36",
                                readCert("4267304698.crt")));

        CompositeTrustStore.init(certs);
    }

    private static byte[] readCert(String path) {
        try (InputStream is =
                KeyStoreLoader.class.getResourceAsStream("/certificates/%s".formatted(path))) {
            if (is == null) {
                throw new IllegalArgumentException("Certificate not found: " + path);
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read certificate: " + path, e);
        }
    }
}
