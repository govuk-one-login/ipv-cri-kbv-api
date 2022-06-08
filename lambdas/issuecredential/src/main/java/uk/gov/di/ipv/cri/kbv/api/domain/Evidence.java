package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonGetter;

public class Evidence {
    private String txn;
    private Integer verificationScore;

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
}
