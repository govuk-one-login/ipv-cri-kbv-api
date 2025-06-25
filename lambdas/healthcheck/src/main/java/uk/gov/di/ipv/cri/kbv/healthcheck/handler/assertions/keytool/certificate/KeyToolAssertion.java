package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.certificate;

import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.FailReport;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keystore.Keystore;
import uk.gov.di.ipv.cri.kbv.healthcheck.util.keytool.Keytool;

import java.io.IOException;
import java.util.Map;

public class KeyToolAssertion implements Assertion {

    private final String keystorePassword;
    private final String jksFileLocation;

    public KeyToolAssertion(String keystore, String keystorePassword) throws IOException {
        this.keystorePassword = keystorePassword;
        this.jksFileLocation = Keystore.createKeyStoreFile(keystore);
    }

    @Override
    public Report run() {
        Report report = new Report();

        try {
            String importResult =
                    Keytool.importCertificate(
                            "/tmp/%d.pfx".formatted(System.currentTimeMillis()),
                            jksFileLocation,
                            keystorePassword);

            Map<String, Object> attributes = KeyToolOutputProcessor.processOutput(importResult);
            report.addAttributes("keytool", attributes);
            report.setPassed(importResult.contains("successfully imported"));
        } catch (Exception e) {
            return new FailReport(e);
        }
        return report;
    }
}
