package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

public class KeyStoreService {
    private final SecretsProvider secretsProvider;
    public static final String KBV_API_KEYSTORE =
            "/dev/kbv-cri-api/experian/keystore";
    public static final String KBV_API_KEYSTORE_PASSWORD =
            "/dev/kbv-cri-api/experian/keystore-password";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreService.class);

    public KeyStoreService(SecretsProvider secretsProvider) {
        this.secretsProvider = secretsProvider;
    }

    public String getKeyStorePath() {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile, Base64.getDecoder().decode(secretsProvider.get(KBV_API_KEYSTORE)));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException | IOException e) {
            LOGGER.error("Initialisation failed", e);
            return null;
        }
    }

    public String getPassword() {
        return secretsProvider.get(KBV_API_KEYSTORE_PASSWORD);
    }
}
