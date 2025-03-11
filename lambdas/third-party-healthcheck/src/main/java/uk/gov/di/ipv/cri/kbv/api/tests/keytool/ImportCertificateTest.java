package uk.gov.di.ipv.cri.kbv.api.tests.keytool;

import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.report.ImportCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.utils.bash.Bash;

public class ImportCertificateTest implements Test<ImportCertificateTestReport> {
    private final String keystorePassword;

    public ImportCertificateTest(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public ImportCertificateTestReport run() {
        String importResult = importCertificate(keystorePassword);
        return new ImportCertificateTestReport(importResult.contains("successfully imported"));
    }

    private static String importCertificate(String keystorePassword) {
        String command =
                String.format(
                        "keytool -importkeystore -srckeystore %s -destkeystore %s -srcstoretype JKS "
                                + "-deststoretype PKCS12 -deststorepass %s -srcstorepass %s -noprompt",
                        Configuration.JKS_FILE_LOCATION,
                        Configuration.PFX_FILE_LOCATION,
                        keystorePassword,
                        keystorePassword);

        try {
            return Bash.execute(command);
        } catch (Exception e) {
            throw new SecurityException("Failed to import certificate", e);
        }
    }
}
