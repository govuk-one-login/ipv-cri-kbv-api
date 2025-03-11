package uk.gov.di.ipv.cri.kbv.api.tests.keystore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeytoolListTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.service.Keytool;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.KeyToolReportGenerator;
import uk.gov.di.ipv.cri.kbv.api.utils.keystore.KeystoreFile;

import java.io.IOException;

public class KeyStoreTest implements Test<KeytoolListTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(KeyStoreTest.class);
    private final String keystoreFile;
    private final String keystorePassword;

    public KeyStoreTest(String keystore, String keystorePassword) throws IOException {
        this.keystorePassword = keystorePassword;
        this.keystoreFile = KeystoreFile.createKeyStoreFile(keystore);
    }

    @Override
    public KeytoolListTestReport run() {
        String keystoreContent = Keytool.getKeyStoreContents(keystoreFile, keystorePassword);
        LOGGER.info("Keystore contents:\n{}", keystoreContent);
        LOGGER.info("Generating keytool list report");
        return KeyToolReportGenerator.generateKeyToolReport(keystoreContent);
    }
}
