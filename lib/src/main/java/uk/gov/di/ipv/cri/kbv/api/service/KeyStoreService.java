package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class KeyStoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreService.class);
    private final SecretsProvider secretsProvider;
    private final String secretKeyPrefix;

    public KeyStoreService() {
        this(
                ParamManager.getSecretsProvider(),
                System.getenv(ConfigurationConstants.AWS_STACK_NAME));
    }

    @ExcludeFromGeneratedCoverageReport
    public KeyStoreService(SecretsProvider secretsProvider, String secretKeyPrefix) {
        this.secretsProvider =
                Objects.requireNonNull(secretsProvider, "secretsProvider must not be null");
        this.secretKeyPrefix =
                Objects.requireNonNull(secretKeyPrefix, "secretKeyPrefix must not be null");
    }

    public String getKeyStorePath() {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(secretsProvider.get(getSecretKey("experian/keystore"))));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException | IOException e) {
            LOGGER.error("Persist keystore to file failed", e);
            return null;
        }
    }

    public String getPassword() {
        return secretsProvider.get(getSecretKey("experian/keystore-password"));
    }

    private String getSecretKey(String keySuffix) {
        return String.format("/%s/%s", secretKeyPrefix, keySuffix);
    }
}
