package uk.gov.di.ipv.cri.kbv.api.tests.keytool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.report.ImportCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.Keytool;
import uk.gov.di.ipv.cri.kbv.api.utils.keystore.KeystoreFile;

import java.io.IOException;

public class ImportCertificateTest implements Test<ImportCertificateTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(ImportCertificateTest.class);
    private final String keystorePassword;
    private final String jksFileLocation;

    public ImportCertificateTest(String keystore, String keystorePassword) throws IOException {
        this.keystorePassword = keystorePassword;
        this.jksFileLocation = KeystoreFile.createKeyStoreFile(keystore);
    }

    @Override
    public ImportCertificateTestReport run() {
        String importResult =
                Keytool.importCertificate(
                        System.currentTimeMillis() + ".pfx", jksFileLocation, keystorePassword);
        boolean success = importResult.contains("successfully imported");
        LOGGER.info("Certificate import {}: {}", success ? "successful" : "failed", importResult);
        return new ImportCertificateTestReport(success);
    }
}
