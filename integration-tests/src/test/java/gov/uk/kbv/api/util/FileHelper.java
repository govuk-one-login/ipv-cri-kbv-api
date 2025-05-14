package gov.uk.kbv.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class FileHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode loadOverrideFile(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.equals("DEFAULT.json")) {
            return null;
        }
        try (InputStream input =
                FileHelper.class.getClassLoader().getResourceAsStream("overrides/" + fileName)) {
            if (input == null) {
                return null;
            }

            JsonNode overrideFile = objectMapper.readTree(input);
            return overrideFile.get("shared_claims");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load override file: " + fileName, e);
        }
    }
}
