package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class KeyStoreService {
    private final SecretsProvider secretsProvider;
    public static final String KBV_API_KEYSTORE_SUFFIX = "/experian/keystore";
    public static final String KBV_API_KEYSTORE_PASSWORD_SUFFIX = "/experian/keystore-password";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreService.class);
    private final String stackName;

    public KeyStoreService(SecretsProvider secretsProvider, String stackName) {
        this.secretsProvider = secretsProvider;
        this.stackName = stackName;
    }

    @ExcludeFromGeneratedCoverageReport
    public KeyStoreService(SecretsProvider secretsProvider) {
        this.secretsProvider = secretsProvider;
        this.stackName =
                Objects.requireNonNull(
                        System.getenv("AWS_STACK_NAME"), "env var AWS_STACK_NAME required");
    }

    public String getKeyStorePath() {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder()
                            .decode(secretsProvider.get(getSecretPath(KBV_API_KEYSTORE_SUFFIX))));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException | IOException e) {
            LOGGER.error("Initialisation failed", e);
            return null;
        }
    }

    public String getPassword() {
        return secretsProvider.get(getSecretPath(KBV_API_KEYSTORE_PASSWORD_SUFFIX));
    }

    private String getSecretPath(String suffix) {
        return String.format("/%s%s", stackName, suffix);
    }
}
