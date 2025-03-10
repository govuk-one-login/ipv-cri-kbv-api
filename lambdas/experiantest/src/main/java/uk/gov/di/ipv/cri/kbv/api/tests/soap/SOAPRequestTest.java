package uk.gov.di.ipv.cri.kbv.api.tests.soap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.service.LoginWithCertificate;

public class SOAPRequestTest implements Test<LoginWithCertificateTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(SOAPRequestTest.class);
    private final String keystorePassword;
    private final String waspUrl;

    public SOAPRequestTest(String keystorePassword, String waspUrl) {
        this.keystorePassword = keystorePassword;
        this.waspUrl = waspUrl;
    }

    @Override
    public LoginWithCertificateTestReport run() {
        LoginWithCertificateTestReport report =
                LoginWithCertificate.performRequest(
                        Configuration.PFX_FILE_LOCATION, keystorePassword, waspUrl);
        LOGGER.info("SOAP request status: HTTP {}", report.getStatusCode());
        return report;
    }
}
