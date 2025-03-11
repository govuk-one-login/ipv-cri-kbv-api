package uk.gov.di.ipv.cri.kbv.api.tests.keystore.report;

import java.util.Map;

public class KeystoreCertificate {
    private String owner;
    private String issuer;
    private String serialNumber;
    private String validFrom;
    private Map<String, String> fingerprints;
    private String signatureAlgorithm;
    private String subjectPublicKeyAlgorithm;
    private int version;

    public KeystoreCertificate() {
        // Empty constructor for Jackson
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public Map<String, String> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(Map<String, String> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSubjectPublicKeyAlgorithm() {
        return subjectPublicKeyAlgorithm;
    }

    public void setSubjectPublicKeyAlgorithm(String subjectPublicKeyAlgorithm) {
        this.subjectPublicKeyAlgorithm = subjectPublicKeyAlgorithm;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
