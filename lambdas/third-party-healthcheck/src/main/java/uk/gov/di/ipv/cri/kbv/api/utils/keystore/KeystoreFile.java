package uk.gov.di.ipv.cri.kbv.api.utils.keystore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class KeystoreFile {
    private static final Logger LOGGER = LogManager.getLogger(KeystoreFile.class);

    private KeystoreFile() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String createKeyStoreFile(String base64KeyStore) throws IOException {
        String keystoreFile = System.currentTimeMillis() + ".jks";

        LOGGER.info("Initializing keystore at: {}", keystoreFile);

        try {
            Path path = Paths.get(keystoreFile).normalize(); // NOSONAR
            byte[] decodedBytes = Base64.getDecoder().decode(base64KeyStore);
            Files.write(path, decodedBytes);
            LOGGER.info("Keystore file created successfully at: {}", keystoreFile);
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to decode Base64 keystore content", e);
        }

        return keystoreFile;
    }
}
