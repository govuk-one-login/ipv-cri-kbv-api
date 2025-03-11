package uk.gov.di.ipv.cri.kbv.api.tests.keytool.service;

import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeyStoreEntry;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeystoreCertificate;
import uk.gov.di.ipv.cri.kbv.api.tests.keystore.report.KeytoolListTestReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyToolReportGenerator {
    private static final String SPLIT_DELIMITER = ":";
    private static final String ENTRY_COUNT_PREFIX = "Your keystore contains";
    private static final String ALIAS_PREFIX = "Alias name:";
    private static final String VALID_FROM_PREFIX = "Valid from";

    private KeyToolReportGenerator() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static KeytoolListTestReport generateKeyToolReport(String output) {
        Objects.requireNonNull(output, "Output string cannot be null");

        KeytoolListTestReport report = initializeReport();
        String[] lines = splitOutputIntoLines(output);

        try {
            processLines(lines, report);
        } catch (Exception e) {
            throw new KeytoolReportGenerationException("Failed to generate keytool report", e);
        }

        return report;
    }

    private static KeytoolListTestReport initializeReport() {
        KeytoolListTestReport report = new KeytoolListTestReport();
        report.setKeystoreEntries(new ArrayList<>());
        return report;
    }

    private static String[] splitOutputIntoLines(String output) {
        return output.split("\n");
    }

    private static void processLines(String[] lines, KeytoolListTestReport report) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith("Keystore type:")) {
                report.setKeystoreType(extractValue(line));
            } else if (line.startsWith("Keystore provider:")) {
                report.setKeystoreProvider(extractValue(line));
            } else if (line.startsWith(ENTRY_COUNT_PREFIX)) {
                report.setNumberOfEntries(parseEntryCount(line));
            } else if (line.startsWith(ALIAS_PREFIX)) {
                i = processKeyStoreEntry(lines, i, report);
            }
        }
    }

    private static int processKeyStoreEntry(
            String[] lines, int startIndex, KeytoolListTestReport report) {
        KeyStoreEntry entry = new KeyStoreEntry();
        report.getKeystoreEntries().add(entry);

        int i = startIndex;
        entry.setAliasName(extractValue(lines[i++]));
        entry.setCreationDate(extractValue(lines[i++]));
        entry.setEntryType(extractValue(lines[i++]));
        entry.setCertificateChainLength(parseIntValue(lines[i++]));

        i++;

        List<KeystoreCertificate> certificates =
                processCertificates(lines, i, entry.getCertificateChainLength());
        entry.setKeystoreCertificates(certificates);

        return i + certificates.size() * 9; // Approximate lines per certificate
    }

    private static List<KeystoreCertificate> processCertificates(
            String[] lines, int startIndex, int count) {
        List<KeystoreCertificate> certificates = new ArrayList<>();
        int i = startIndex;

        for (int j = 0; j < count; j++) {
            KeystoreCertificate cert = new KeystoreCertificate();

            cert.setOwner(extractValue(lines[i++]));
            cert.setIssuer(extractValue(lines[i++]));
            cert.setSerialNumber(extractValue(lines[i++]));
            cert.setValidFrom(lines[i++].replaceFirst(VALID_FROM_PREFIX, "").trim());

            i++; // Skip "Certificate fingerprints:"
            Map<String, String> fingerprints = extractFingerprints(lines, i);
            i += fingerprints.size();

            cert.setFingerprints(fingerprints);
            cert.setSignatureAlgorithm(extractValue(lines[i++]));
            cert.setSubjectPublicKeyAlgorithm(extractValue(lines[i++]));
            cert.setVersion(parseIntValue(lines[i++]));

            certificates.add(cert);
        }

        return certificates;
    }

    private static Map<String, String> extractFingerprints(String[] lines, int startIndex) {
        Map<String, String> fingerprints = new HashMap<>();
        int i = startIndex;

        while (!lines[i].startsWith("Signature algorithm name")) {
            String line = lines[i].trim();
            String[] parts = line.split(SPLIT_DELIMITER, 2);
            fingerprints.put(parts[0].trim(), parts[1].trim());
            i++;
        }

        return fingerprints;
    }

    private static String extractValue(String line) {
        return line.split(SPLIT_DELIMITER, 2)[1].trim();
    }

    private static int parseIntValue(String line) {
        return Integer.parseInt(extractValue(line));
    }

    private static int parseEntryCount(String line) {
        String[] parts = line.split("\\s+");
        return Integer.parseInt(parts[3]);
    }

    public static class KeytoolReportGenerationException extends RuntimeException {
        public KeytoolReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
