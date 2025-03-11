package uk.gov.di.ipv.cri.kbv.api.tests.keystore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeytoolListTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.service.Keytool;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.KeyToolReportGenerator;

public class KeyStoreTest implements Test<KeytoolListTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(KeyStoreTest.class);
    private final String keystorePassword;

    public KeyStoreTest(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public KeytoolListTestReport run() {
        String keystoreContent =
                Keytool.getKeyStoreContents(Configuration.JKS_FILE_LOCATION, keystorePassword);
        LOGGER.info("Keystore contents:\n{}", keystoreContent);
        LOGGER.info("Generating keytool list report");
        return KeyToolReportGenerator.generateKeyToolReport(keystoreContent);
    }
}
