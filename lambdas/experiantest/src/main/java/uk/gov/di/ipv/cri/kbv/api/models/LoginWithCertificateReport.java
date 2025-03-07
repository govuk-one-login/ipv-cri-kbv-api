package uk.gov.di.ipv.cri.kbv.api.models;

import java.util.List;
import java.util.Map;

public class LoginWithCertificateReport {
    private int statusCode;
    private long date;
    private int contentLength;
    private Map<String, List<String>> headers;
    private boolean soapTokenValid;

    public LoginWithCertificateReport() {
        // Empty constructor for Jackson
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public boolean isSoapTokenValid() {
        return soapTokenValid;
    }

    public void setSoapTokenValid(boolean soapTokenValid) {
        this.soapTokenValid = soapTokenValid;
    }
}
