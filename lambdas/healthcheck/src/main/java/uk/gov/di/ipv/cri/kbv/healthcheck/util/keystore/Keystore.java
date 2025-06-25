package uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Keystore {
    private static final Logger LOGGER = LoggerFactory.getLogger(Keystore.class);

    private Keystore() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String createKeyStoreFile(String base64KeyStore) throws IOException {
        String keystoreFile = "/tmp/" + System.currentTimeMillis() + ".jks";

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
