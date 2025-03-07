package uk.gov.di.ipv.cri.kbv.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;

public final class Keytool {
    private static final Logger LOGGER = LoggerFactory.getLogger(Keytool.class);
    private static final String KEYTOOL_CMD = "keytool";
    private static final String JKS_TYPE = "JKS";
    private static final String PKCS12_TYPE = "PKCS12";

    private Keytool() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static void createKeyStoreFile(String keyStoreLocation, String base64KeyStore)
            throws IOException {
        validateInput(keyStoreLocation, "keyStoreLocation");
        validateInput(base64KeyStore, "base64KeyStore");

        try {
            Path path = Paths.get(keyStoreLocation);
            byte[] decodedBytes = Base64.getDecoder().decode(base64KeyStore);
            Files.write(path, decodedBytes);
            LOGGER.debug("Keystore file created successfully at: {}", keyStoreLocation);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid Base64 encoding in keystore content", e);
            throw new IOException("Failed to decode Base64 keystore content", e);
        }
    }

    public static String importCertificate(
            String sourceKeystore, String destKeystore, String keystorePassword) {
        validateInput(sourceKeystore, "sourceKeystore");
        validateInput(destKeystore, "destKeystore");
        validateInput(keystorePassword, "keystorePassword");

        String command =
                String.format(
                        "%s -importkeystore -srckeystore %s -destkeystore %s -srcstoretype %s "
                                + "-deststoretype %s -deststorepass %s -srcstorepass %s -noprompt",
                        KEYTOOL_CMD,
                        sourceKeystore,
                        destKeystore,
                        JKS_TYPE,
                        PKCS12_TYPE,
                        keystorePassword,
                        keystorePassword);

        return executeCommand(command, "Failed to import certificate");
    }

    public static String getKeyStoreContents(String keystore, String keystorePassword) {
        validateInput(keystore, "keystore");
        validateInput(keystorePassword, "keystorePassword");

        String command =
                String.format(
                        "%s -list -v -keystore %s -storepass %s",
                        KEYTOOL_CMD, keystore, keystorePassword);

        return executeCommand(command, "Failed to list keystore contents");
    }

    private static void validateInput(String input, String paramName) {
        if (Objects.isNull(input) || input.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    private static String executeCommand(String command, String errorMessage) {
        try {
            LOGGER.debug("Executing command: {}", command);
            String result = Bash.execute(command);
            LOGGER.debug("Command executed successfully");
            return result;
        } catch (Exception e) {
            LOGGER.error("{}: {}", errorMessage, e.getMessage());
            throw new SecurityException(errorMessage, e);
        }
    }
}
