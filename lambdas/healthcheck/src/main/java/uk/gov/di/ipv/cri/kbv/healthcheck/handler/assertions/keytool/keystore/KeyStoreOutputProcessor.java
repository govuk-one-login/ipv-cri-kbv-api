package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.keystore;

import uk.gov.di.ipv.cri.kbv.healthcheck.util.StringMasking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyStoreOutputProcessor {
    private static final String SPLIT_DELIMITER = ":";
    private static final String ENTRY_COUNT_PREFIX = "Your keystore contains";
    private static final String ALIAS_PREFIX = "Alias name:";
    private static final String CERTIFICATE_CHAIN_LEN_PREFIX = "certificateChainLength";
    private static final String VALID_FROM_PREFIX = "Valid from:";
    private static final String SEPARATOR = "*******************************************";
    private static final Pattern FINGERPRINT_PATTERN =
            Pattern.compile("\\s*(SHA1|SHA256)\\s*:\\s*([A-F0-9:]+)"); // NOSONAR

    private KeyStoreOutputProcessor() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static Map<String, Object> processOutput(String output) {
        Map<String, Object> attributes = new HashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        String[] lines = output.split("\n");
        int i = 0;

        while (i < lines.length && !lines[i].startsWith(ENTRY_COUNT_PREFIX)) {
            String line = lines[i].trim();
            if (line.startsWith("Keystore type:")) {
                attributes.put("keystoreType", extractValue(line));
            }
            i++;
        }

        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.startsWith(ALIAS_PREFIX)) {
                Map<String, Object> entry = parseEntry(lines, i);
                entries.add(entry);
                while (i < lines.length && !lines[i].trim().equals(SEPARATOR)) {
                    i++;
                }
                i += 2;
            } else {
                i++;
            }
        }

        attributes.put("entries", entries);
        return attributes;
    }

    private static Map<String, Object> parseEntry(String[] lines, int startIndex) {
        Map<String, Object> entry = new HashMap<>();
        int i = startIndex;

        entry.put("aliasName", extractValue(lines[i++].trim()));
        i = parseEntryMeta(lines, i, entry);

        if (i >= lines.length || lines[i].trim().equals(SEPARATOR)) {
            return entry;
        }

        int chainLength = (int) entry.getOrDefault(CERTIFICATE_CHAIN_LEN_PREFIX, 1);
        List<Map<String, Object>> certificates = parseCertificates(lines, i, chainLength);
        if (!certificates.isEmpty()) {
            entry.put("certificates", certificates);
        }

        return entry;
    }

    private static int parseEntryMeta(String[] lines, int i, Map<String, Object> entry) {
        while (i < lines.length && !lines[i].trim().equals(SEPARATOR)) {
            String line = lines[i].trim();

            if (line.startsWith("Creation date:")) {
                entry.put("creationDate", extractValue(line));
            } else if (line.startsWith("Certificate chain length:")) {
                return i + 1;
            }
            i++;
        }
        return i;
    }

    private static List<Map<String, Object>> parseCertificates(
            String[] lines, int startIndex, int chainLength) {
        List<Map<String, Object>> certificates = new ArrayList<>();
        int i = startIndex;

        for (int j = 0; j < chainLength && i < lines.length; j++) {
            ParseResult result = parseCertificateBlock(lines, i, j + 1);
            if (!result.certificate.isEmpty()) {
                certificates.add(result.certificate);
            }
            i = result.nextIndex;
        }

        return certificates;
    }

    private static ParseResult parseCertificateBlock(
            String[] lines, int startIndex, int certIndex) {
        Map<String, Object> cert = new HashMap<>();
        boolean inCertificate = false;
        int i = startIndex;

        while (i < lines.length
                && !lines[i].trim().equals(SEPARATOR)
                && !lines[i].startsWith(ALIAS_PREFIX)) {
            String line = lines[i].trim();

            if (line.startsWith("Certificate[" + certIndex + "]:")) {
                inCertificate = true;
                i++;
                continue;
            }

            if (inCertificate) {
                i = processCertificateLine(lines, i, cert);
            } else {
                i++;
            }
        }

        return new ParseResult(cert, i);
    }

    private static int processCertificateLine(String[] lines, int index, Map<String, Object> cert) {
        String line = lines[index].trim();

        if (line.startsWith("Owner:")) {
            cert.put("owner", extractValue(line));
        } else if (line.startsWith("Issuer:")) {
            cert.put("issuer", extractValue(line));
        } else if (line.startsWith("Serial number:")) {
            cert.put("serialNumber", extractValue(line));
        } else if (line.startsWith(VALID_FROM_PREFIX)) {
            cert.put("validFrom", extractValue(line));
        } else if (line.startsWith("Certificate fingerprints:")) {
            cert.put("fingerprints", extractFingerprints(lines, ++index));
            while (index < lines.length
                    && !lines[index].trim().startsWith("Signature algorithm name:")) {
                index++;
            }
        } else if (line.startsWith("Version:")) {
            try {
                cert.put("version", Integer.parseInt(extractValue(line)));
            } catch (NumberFormatException e) {
                cert.put("version", 0);
            }
        }

        return index + 1;
    }

    private static Map<String, String> extractFingerprints(String[] lines, int startIndex) {
        Map<String, String> fingerprints = new HashMap<>();
        int i = startIndex;

        while (i < lines.length
                && !lines[i].trim().isEmpty()
                && !lines[i].trim().startsWith("Signature algorithm name:")) {
            Matcher matcher = FINGERPRINT_PATTERN.matcher(lines[i].trim());
            if (matcher.find()) {
                String value = matcher.group(2);
                fingerprints.put(
                        matcher.group(1),
                        StringMasking.maskString(value, Math.max(0, value.length() / 3)));
            }
            i++;
        }

        return fingerprints;
    }

    private static String extractValue(String line) {
        String[] parts = line.split(SPLIT_DELIMITER, 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }
}
