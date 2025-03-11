package uk.gov.di.ipv.cri.kbv.api.tests.keytool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.report.ImportCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.Keytool;

public class ImportCertificateTest implements Test<ImportCertificateTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(ImportCertificateTest.class);
    private final String keystorePassword;

    public ImportCertificateTest(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public ImportCertificateTestReport run() {
        String importResult =
                Keytool.importCertificate(
                        Configuration.JKS_FILE_LOCATION,
                        Configuration.PFX_FILE_LOCATION,
                        keystorePassword);
        boolean success = importResult.contains("successfully imported");
        LOGGER.info("Certificate import {}: {}", success ? "successful" : "failed", importResult);
        return new ImportCertificateTestReport(success);
    }
}
