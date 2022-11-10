package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonGetter;

public class Evidence {
    private String txn;
    private Integer verificationScore;
    private ContraIndicator[] ci;
    private CheckDetail[] checkDetails;
    private CheckDetail[] failedCheckDetails;

    public String getTxn() {
        return txn;
    }

    public void setTxn(String txn) {
        this.txn = txn;
    }

    @JsonGetter("type")
    public String getType() {
        return VerifiableCredentialConstants.VC_EVIDENCE_TYPE;
    }

    public Integer getVerificationScore() {
        return verificationScore;
    }

    public void setVerificationScore(Integer verificationScore) {
        this.verificationScore = verificationScore;
    }

    public void setCi(ContraIndicator[] ci) {
        this.ci = ci;
    }

    public ContraIndicator[] getCi() {
        return ci;
    }

    public CheckDetail[] getCheckDetails() {
        return checkDetails;
    }

    public void setCheckDetails(CheckDetail[] checkDetails) {
        this.checkDetails = checkDetails;
    }

    public CheckDetail[] getFailedCheckDetails() {
        return failedCheckDetails;
    }

    public void setFailedCheckDetails(CheckDetail[] failedCheckDetails) {
        this.failedCheckDetails = failedCheckDetails;
    }
}
