package uk.gov.di.ipv.cri.kbv.api.tests.assertions;

import uk.gov.di.ipv.cri.kbv.api.handler.Configuration;
import uk.gov.di.ipv.cri.kbv.api.tests.Test;
import uk.gov.di.ipv.cri.kbv.api.tests.assertions.report.AssertionsReport;
import uk.gov.di.ipv.cri.kbv.api.tests.keytool.report.ImportCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.soap.report.LoginWithCertificateTestReport;
import uk.gov.di.ipv.cri.kbv.api.tests.ssl.report.SSLHandshakeTestReport;

import java.nio.file.Paths;

public class AssertionTest implements Test<AssertionsReport> {
    private final SSLHandshakeTestReport sslHandshakeReport;
    private final LoginWithCertificateTestReport loginWithCertificateReport;
    private final ImportCertificateTestReport importCertificateReport;

    public AssertionTest(Object... reports) {
        if (reports == null) {
            throw new IllegalArgumentException("Reports list cannot be null");
        }

        SSLHandshakeTestReport sslReport = null;
        LoginWithCertificateTestReport loginReport = null;
        ImportCertificateTestReport importReport = null;

        for (Object report : reports) {
            if (report != null) {
                if (report instanceof SSLHandshakeTestReport) {
                    sslReport = (SSLHandshakeTestReport) report;
                } else if (report instanceof LoginWithCertificateTestReport) {
                    loginReport = (LoginWithCertificateTestReport) report;
                } else if (report instanceof ImportCertificateTestReport) {
                    importReport = (ImportCertificateTestReport) report;
                }
            }
        }

        this.sslHandshakeReport = sslReport;
        this.loginWithCertificateReport = loginReport;
        this.importCertificateReport = importReport;
    }

    @Override
    public AssertionsReport run() {
        AssertionsReport assertionsReport = new AssertionsReport();

        assertionsReport.setSslConnection(evaluateSslConnection());
        assertionsReport.setSoapTokenRequest(evaluateSoapTokenRequest());
        assertionsReport.setKeystoreImport(evaluateKeystoreImport());
        assertionsReport.setJksLoaded(checkJksFileExists());
        assertionsReport.setSoapTokenValid(evaluateSoapTokenValid());

        return assertionsReport;
    }

    private String evaluateSslConnection() {
        return toStatus(sslHandshakeReport != null && sslHandshakeReport.isSessionValid());
    }

    private String evaluateSoapTokenRequest() {
        return toStatus(
                loginWithCertificateReport != null
                        && loginWithCertificateReport.getStatusCode() == 200);
    }

    private String evaluateKeystoreImport() {
        return toStatus(importCertificateReport != null && importCertificateReport.isSuccess());
    }

    private String checkJksFileExists() {
        return toStatus(
                Paths.get(Configuration.JKS_FILE_LOCATION) // NOSONAR
                        .normalize()
                        .toFile()
                        .exists());
    }

    private String evaluateSoapTokenValid() {
        return toStatus(
                loginWithCertificateReport != null
                        && loginWithCertificateReport.isSoapTokenValid());
    }

    private static String toStatus(boolean condition) {
        return condition ? "success" : "failed";
    }
}
