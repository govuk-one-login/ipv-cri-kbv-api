package uk.gov.di.ipv.cri.kbv.api.tests.soap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.service.Keytool;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.service.LoginWithCertificate;
import uk.gov.di.ipv.cri.kbv.api.utils.keystore.KeystoreFile;

import java.io.IOException;

public class SOAPRequestTest implements Test<LoginWithCertificateTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(SOAPRequestTest.class);
    private final String keystorePassword;
    private final String waspUrl;
    private final String jksFileLocation;

    public SOAPRequestTest(String keystore, String keystorePassword, String waspUrl)
            throws IOException {
        this.keystorePassword = keystorePassword;
        this.waspUrl = waspUrl;
        this.jksFileLocation = KeystoreFile.createKeyStoreFile(keystore);
    }

    @Override
    public LoginWithCertificateTestReport run() {
        String pfx = "/tmp/" + System.currentTimeMillis() + ".pfx";

        Keytool.importCertificate(pfx, jksFileLocation, keystorePassword);

        LoginWithCertificateTestReport report =
                LoginWithCertificate.performRequest(pfx, keystorePassword, waspUrl);

        LOGGER.info("SOAP request status: HTTP {}", report.getStatusCode());
        return report;
    }
}
