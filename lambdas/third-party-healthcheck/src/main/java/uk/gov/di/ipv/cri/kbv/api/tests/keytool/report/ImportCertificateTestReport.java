package uk.gov.di.ipv.cri.kbv.api.tests.keytool.report;

public class ImportCertificateTestReport {
    private boolean success;

    public ImportCertificateTestReport() {
        // Empty constructor for Jackson
    }

    public ImportCertificateTestReport(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
