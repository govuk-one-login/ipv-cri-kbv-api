package uk.gov.di.ipv.cri.kbv.api.tests.keystore.report;

import java.util.List;

public class KeytoolListTestReport {
    private String keystoreType;
    private String keystoreProvider;
    private int numberOfEntries;
    private List<KeyStoreEntry> keystoreEntries;

    public KeytoolListTestReport() {
        // Empty constructor for Jackson
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getKeystoreProvider() {
        return keystoreProvider;
    }

    public void setKeystoreProvider(String keystoreProvider) {
        this.keystoreProvider = keystoreProvider;
    }

    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    public void setNumberOfEntries(int numberOfEntries) {
        this.numberOfEntries = numberOfEntries;
    }

    public List<KeyStoreEntry> getKeystoreEntries() {
        return keystoreEntries;
    }

    public void setKeystoreEntries(List<KeyStoreEntry> keystoreEntries) {
        this.keystoreEntries = keystoreEntries;
    }
}
