package uk.gov.di.ipv.cri.kbv.api.utils;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import uk.gov.di.ipv.cri.kbv.api.exceptions.SecretsManagerException;

import java.util.Objects;

public class SecretsGrabber {
    private final SecretsManagerClient secretsManagerClient;

    public SecretsGrabber(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient =
                Objects.requireNonNull(
                        secretsManagerClient, "SecretsManagerClient must not be null");
    }

    public String getSecretValue(String secretName) {
        validateSecretName(secretName);

        try {
            GetSecretValueRequest request =
                    GetSecretValueRequest.builder().secretId(secretName).build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            return response.secretString();

        } catch (Exception e) {
            throw new SecretsManagerException(
                    String.format("Failed to retrieve secret: %s", secretName), e);
        }
    }

    private void validateSecretName(String secretName) {
        Objects.requireNonNull(secretName, "Secret name must not be null");
        if (secretName.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret name must not be empty");
        }
    }
}
