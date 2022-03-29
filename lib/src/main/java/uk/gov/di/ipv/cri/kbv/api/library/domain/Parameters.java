package uk.gov.di.ipv.cri.kbv.api.library.domain;

public class Parameters {

    private String oneShotAuthentication;
    private String storeCaseData;

    public String getOneShotAuthentication() {
        return oneShotAuthentication;
    }

    public void setOneShotAuthentication(String oneShotAuthentication) {
        this.oneShotAuthentication = oneShotAuthentication;
    }

    public String getStoreCaseData() {
        return storeCaseData;
    }

    public void setStoreCaseData(String storeCaseData) {
        this.storeCaseData = storeCaseData;
    }
}
