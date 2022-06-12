package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class KeyStoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreService.class);
    private AWSSecretsRetriever awsSecretsRetriever;

    public KeyStoreService() {
        this(new AWSSecretsRetriever());
    }

    @ExcludeFromGeneratedCoverageReport
    public KeyStoreService(AWSSecretsRetriever awsSecretsRetriever) {
        this.awsSecretsRetriever =
                Objects.requireNonNull(awsSecretsRetriever, "awsSecretsRetriever must not be null");
    }

    public String getKeyStorePath() {
        try {
            File file = Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
            Path tempFile = file.toPath();
            Files.write(
                    tempFile,
                    Base64.getDecoder().decode(awsSecretsRetriever.getValue("experian/keystore")));
            return tempFile.toString();
        } catch (IllegalArgumentException | NullPointerException | IOException e) {
            LOGGER.error("Persist keystore to file failed", e);
            return null;
        }
    }

    public String getPassword() {
        return awsSecretsRetriever.getValue("experian/keystore-password");
    }
}
