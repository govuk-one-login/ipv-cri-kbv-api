package uk.gov.di.ipv.cri.kbv.api.models;

public class LambdaReport {
    private Assertions assertions;
    private SSLHandshakeReport sslHandshakeReport;
    private LoginWithCertificateReport soapReport;

    public LambdaReport() {
        // Empty constructor for Jackson
    }

    public LambdaReport(
            SSLHandshakeReport sslHandshakeReport,
            LoginWithCertificateReport soapReport,
            Assertions assertions) {
        this.sslHandshakeReport = sslHandshakeReport;
        this.soapReport = soapReport;
        this.assertions = assertions;
    }

    public SSLHandshakeReport getSslHandshakeReport() {
        return sslHandshakeReport;
    }

    public void setSslHandshakeReport(SSLHandshakeReport sslHandshakeReport) {
        this.sslHandshakeReport = sslHandshakeReport;
    }

    public LoginWithCertificateReport getSoapReport() {
        return soapReport;
    }

    public void setSoapReport(LoginWithCertificateReport soapReport) {
        this.soapReport = soapReport;
    }

    public Assertions getAssertions() {
        return assertions;
    }

    public void setAssertions(Assertions assertions) {
        this.assertions = assertions;
    }
}
