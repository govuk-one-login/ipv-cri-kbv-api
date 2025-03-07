package uk.gov.di.ipv.cri.kbv.api.tests.keytool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.utils.bash.Bash;

import java.util.Objects;

public class Keytool {
    private static final Logger LOGGER = LoggerFactory.getLogger(Keytool.class);

    private Keytool() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String importCertificate(
            String sourceKeystore, String destKeystore, String keystorePassword) {
        validateInput(sourceKeystore, "sourceKeystore");
        validateInput(destKeystore, "destKeystore");
        validateInput(keystorePassword, "keystorePassword");

        String command =
                String.format(
                        "keytool -importkeystore -srckeystore %s -destkeystore %s -srcstoretype JKS "
                                + "-deststoretype PKCS12 -deststorepass %s -srcstorepass %s -noprompt",
                        sourceKeystore, destKeystore, keystorePassword, keystorePassword);

        try {
            return Bash.execute(command);
        } catch (Exception e) {
            throw new SecurityException("Failed to import certificate", e);
        }
    }

    private static void validateInput(String input, String paramName) {
        if (Objects.isNull(input) || input.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }
}
