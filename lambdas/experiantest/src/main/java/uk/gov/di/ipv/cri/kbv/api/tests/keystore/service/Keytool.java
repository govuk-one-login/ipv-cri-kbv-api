package uk.gov.di.ipv.cri.kbv.api.tests.keystore.service;

import uk.gov.di.ipv.cri.kbv.api.utils.bash.Bash;

public class Keytool {
    public static String getKeyStoreContents(String keystore, String keystorePassword) {
        try {
            return Bash.execute(
                    String.format(
                            "keytool -list -v -keystore %s -storepass %s",
                            keystore, keystorePassword));
        } catch (Exception e) {
            throw new SecurityException("Failed to list keystore contents", e);
        }
    }
}
