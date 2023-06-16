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
    private static final String KENNETH_DECERQUEIRA = "KennethDecerqueira.json";

    public static String getCorrect(String question) throws IOException {
        return AnswerResource.retrieve(question, CORRECT);
    }

    public static String getInCorrect(String question) throws IOException {
        return AnswerResource.retrieve(question, INCORRECT);
    }

    private static String retrieve(String question, String answerState) throws IOException {
        return Arrays.stream(convert(KENNETH_DECERQUEIRA).get(question).toString().split(";"))
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
}
