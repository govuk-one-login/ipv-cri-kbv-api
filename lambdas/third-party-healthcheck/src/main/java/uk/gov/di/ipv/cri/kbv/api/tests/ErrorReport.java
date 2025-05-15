package uk.gov.di.ipv.cri.kbv.api.tests;

public class ErrorReport {
    private boolean threwException;
    private String error;

    public ErrorReport() {
        // Empty constructor for Jackson
    }

    public ErrorReport(String error, boolean threwException) {
        this.error = error;
        this.threwException = threwException;
    }

    public boolean isThrewException() {
        return threwException;
    }

    public void setThrewException(boolean threwException) {
        this.threwException = threwException;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
