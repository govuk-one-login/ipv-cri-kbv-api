package uk.gov.di.ipv.cri.kbv.api.tests.ssl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.report.SSLHandshakeTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.service.SSLConnection;

import javax.net.ssl.SSLPeerUnverifiedException;

public class SSLHandshakeTest implements Test<SSLHandshakeTestReport> {
    private static final Logger LOGGER = LogManager.getLogger(SSLHandshakeTest.class);

    @Override
    public SSLHandshakeTestReport run() {
        try {
            SSLHandshakeTestReport report =
                    SSLConnection.testSSLHandshake(
                            Configuration.WASP_HOST, Configuration.WASP_PORT);
            LOGGER.info("SSL handshake {}!", report.isSessionValid() ? "successful" : "failed");
            return report;
        } catch (SSLPeerUnverifiedException e) {
            LOGGER.warn("SSL handshake failed due to peer verification", e);
            SSLHandshakeTestReport report = new SSLHandshakeTestReport();
            report.setSessionValid(false);
            return report;
        }
    }
}
