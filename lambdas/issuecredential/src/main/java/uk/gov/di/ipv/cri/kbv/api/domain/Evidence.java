package uk.gov.di.ipv.cri.kbv.api.domain;

public class Evidence {
    private String txn;
    private final String type = VerifiableCredentialConstants.VC_EVIDENCE_TYPE;
    private Integer verificationScore;

    public String getTxn() {
        return txn;
    }

    public void setTxn(String txn) {
        this.txn = txn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {}

    public Integer getVerificationScore() {
        return verificationScore;
    }

    public void setVerificationScore(Integer verificationScore) {
        this.verificationScore = verificationScore;
    }
}
