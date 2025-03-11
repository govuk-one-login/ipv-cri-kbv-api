package uk.gov.di.ipv.cri.kbv.api.tests.assertions.report;

public class Assertions {
    private String sslConnection;
    private String soapTokenRequest;
    private String keystoreImport;
    private String soapTokenValid;

    public Assertions() {
        // Empty constructor for Jackson
    }

    public String getSslConnection() {
        return sslConnection;
    }

    public void setSslConnection(String sslConnection) {
        this.sslConnection = sslConnection;
    }

    public String getSoapTokenRequest() {
        return soapTokenRequest;
    }

    public void setSoapTokenRequest(String soapTokenRequest) {
        this.soapTokenRequest = soapTokenRequest;
    }

    public String getKeystoreImport() {
        return keystoreImport;
    }

    public void setKeystoreImport(String keystoreImport) {
        this.keystoreImport = keystoreImport;
    }

    public String getSoapTokenValid() {
        return soapTokenValid;
    }

    public void setSoapTokenValid(String soapTokenValid) {
        this.soapTokenValid = soapTokenValid;
    }
}
