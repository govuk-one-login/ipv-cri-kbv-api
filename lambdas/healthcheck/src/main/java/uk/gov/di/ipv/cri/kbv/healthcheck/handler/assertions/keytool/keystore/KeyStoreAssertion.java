package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.keystore;

import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore.Keystore;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keytool.Keytool;

import java.io.IOException;
import java.util.Map;

public class KeyStoreAssertion implements Assertion {
    private final String keystoreFile;
    private final String keystorePassword;

    public KeyStoreAssertion(String keystoreFile, String keystorePassword) throws IOException {
        this.keystorePassword = keystorePassword;
        this.keystoreFile = Keystore.createKeyStoreFile(keystoreFile);
    }

    @Override
    public Report run() {
        Report report = new Report();

        try {
            String keyStoreResult = Keytool.getKeyStoreContents(keystoreFile, keystorePassword);
            Map<String, Object> attributes = KeyStoreOutputProcessor.processOutput(keyStoreResult);

            report.addAttributes("keystore", attributes);
            report.setPassed(!attributes.isEmpty());
        } catch (Exception e) {
            return new FailReport(e);
        }

        return report;
    }
}
