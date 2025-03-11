package uk.gov.di.ipv.cri.kbv.api.tests.keystore.report;

import java.util.List;

public class KeyStoreEntry {
    private String aliasName;
    private String creationDate;
    private String entryType;
    private int certificateChainLength;
    private List<KeystoreCertificate> keystoreCertificates;

    public KeyStoreEntry() {
        // Empty constructor for Jackson
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public int getCertificateChainLength() {
        return certificateChainLength;
    }

    public void setCertificateChainLength(int certificateChainLength) {
        this.certificateChainLength = certificateChainLength;
    }

    public List<KeystoreCertificate> getKeystoreCertificates() {
        return keystoreCertificates;
    }

    public void setKeystoreCertificates(List<KeystoreCertificate> keystoreCertificates) {
        this.keystoreCertificates = keystoreCertificates;
    }
}
