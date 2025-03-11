package uk.gov.di.ipv.cri.kbv.api.tests.keystore;

import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeytoolListTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.report.KeyToolReportGenerator;
import uk.gov.di.ipv.cri.kbv.api.utils.bash.Bash;

public class KeyStoreTest implements Test<KeytoolListTestReport> {
    private final String keystorePassword;

    public KeyStoreTest(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public KeytoolListTestReport run() {
        String keystoreContent = getKeyStoreContents(keystorePassword);
        return KeyToolReportGenerator.generateKeyToolReport(keystoreContent);
    }

    private static String getKeyStoreContents(String keystorePassword) {
        try {

            String command =
                    String.format(
                            "keytool -list -v -keystore %s -storepass %s",
                            Configuration.JKS_FILE_LOCATION, keystorePassword);

            return Bash.execute(command);

        } catch (Exception e) {
            throw new SecurityException("Failed to list keystore contents", e);
        }
    }
}
