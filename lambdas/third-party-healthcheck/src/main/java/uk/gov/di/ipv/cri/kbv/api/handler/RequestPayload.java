package uk.gov.di.ipv.cri.kbv.api.handler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestPayload {
    private String host;
    private int port;
    private String base64Keystore;
    private String keystorePassword;
    private List<String> tests;

    public RequestPayload() {
        // Empty constructor for Jackson
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBase64Keystore() {
        return base64Keystore;
    }

    public void setBase64Keystore(String base64Keystore) {
        this.base64Keystore = base64Keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
