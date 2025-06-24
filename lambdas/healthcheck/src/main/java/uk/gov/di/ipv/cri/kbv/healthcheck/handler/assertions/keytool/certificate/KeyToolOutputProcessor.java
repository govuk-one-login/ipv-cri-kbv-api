package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.keytool.certificate;

import java.util.HashMap;
import java.util.Map;

public final class KeyToolOutputProcessor {

    private KeyToolOutputProcessor() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static Map<String, Object> processOutput(String output) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("result", extractImportResult(output));
        attributes.put("entries", extractAliasEntries(output));
        return attributes;
    }

    private static String extractImportResult(String output) {
        for (String line : output.split("\n")) {
            if (line.startsWith("Import command completed")) {
                return line;
            }
        }
        return "No import result found";
    }

    private static Map<String, String> extractAliasEntries(String output) {
        Map<String, String> aliases = new HashMap<>();

        for (String line : output.split("\n")) {
            if (line.startsWith("Entry for")) {
                String alias = extractAlias(line);
                if (alias != null) {
                    String status =
                            line.contains("successfully imported")
                                    ? "successfully imported"
                                    : "failed to import";
                    aliases.put(alias, status);
                }
            }
        }

        return aliases;
    }

    private static String extractAlias(String line) {
        int startIdx = line.indexOf("alias ");
        if (startIdx == -1) {
            return null;
        }
        String[] parts = line.substring(startIdx + "alias ".length()).split(" ");
        return (parts.length > 0) ? parts[0] : null;
    }
}
