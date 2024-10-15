package gov.uk.kbv.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class AnswerResource {
    private static final String CORRECT = "CORRECT";
    private static final String INCORRECT = "INCORRECT";
    private static final Map<Integer, String> resourceMap =
            Map.of(
                    197, "KennethDecerqueira.json",
                    256, "HildaHead.json",
                    1188, "CarolineCoulson.json");

    public static String getCorrect(String question, int user) throws IOException {
        return AnswerResource.retrieve(question, CORRECT, user);
    }

    public static String getInCorrect(String question, int user) throws IOException {
        return AnswerResource.retrieve(question, INCORRECT, user);
    }

    private static String retrieve(String question, String answerState, int user)
            throws IOException {
        String jsonFileName = getResourceFileName(user);
        return Arrays.stream(convert(jsonFileName).get(question).toString().split(";"))
                .filter(item -> item.startsWith(answerState))
                .collect(Collectors.joining())
                .split(":")[1];
    }

    private static Map<String, Object> convert(String resourceName) throws IOException {
        InputStream inputStream =
                AnswerResource.class.getClassLoader().getResourceAsStream(resourceName);
        Reader reader = new InputStreamReader(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(reader, Map.class);

        return map;
    }

    private static String getResourceFileName(int user) throws IOException {
        String jsonFileName = resourceMap.get(user);
        if (jsonFileName == null) {
            throw new IOException("Resource ID not found: " + user);
        }
        return jsonFileName;
    }
}
