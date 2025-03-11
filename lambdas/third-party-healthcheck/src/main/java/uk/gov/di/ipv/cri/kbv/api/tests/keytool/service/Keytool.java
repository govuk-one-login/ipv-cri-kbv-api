package uk.gov.di.ipv.cri.kbv.api.tests.keytool.service;

import uk.gov.di.ipv.cri.kbv.api.utils.bash.Bash;

import java.util.Objects;

public class Keytool {
    private Keytool() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static String importCertificate(
            String outputPfxFileName, String sourceKeystore, String keystorePassword) {
        validateInput(sourceKeystore, "sourceKeystore");
        validateInput(keystorePassword, "keystorePassword");

        String command =
                String.format(
                        "keytool -importkeystore -srckeystore %s -destkeystore %s -srcstoretype JKS "
                                + "-deststoretype PKCS12 -deststorepass %s -srcstorepass %s -noprompt",
                        sourceKeystore, outputPfxFileName, keystorePassword, keystorePassword);

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
