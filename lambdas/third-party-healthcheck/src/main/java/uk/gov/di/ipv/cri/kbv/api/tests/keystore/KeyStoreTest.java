package uk.gov.di.ipv.cri.kbv.api.tests.keystore;

import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeytoolListTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.service.Keytool;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.KeyToolReportGenerator;
import uk.gov.di.ipv.cri.kbv.api.utils.keystore.KeystoreFile;

import java.io.IOException;

public class KeyStoreTest implements Test<KeytoolListTestReport> {
    private final String keystoreFile;
    private final String keystorePassword;

    public KeyStoreTest(String keystore, String keystorePassword) throws IOException {
        this.keystorePassword = keystorePassword;
        this.keystoreFile = KeystoreFile.createKeyStoreFile(keystore);
    }

    @Override
    public KeytoolListTestReport run() {
        String keystoreContent = Keytool.getKeyStoreContents(keystoreFile, keystorePassword);
        return KeyToolReportGenerator.generateKeyToolReport(keystoreContent);
    }
}
