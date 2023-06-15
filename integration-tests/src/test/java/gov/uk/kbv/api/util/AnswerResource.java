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

    public static String getCorrect(String question) throws IOException {
        return AnswerResource.retrieve(question, CORRECT);
    }

    public static String getInCorrect(String question) throws IOException {
        return AnswerResource.retrieve(question, INCORRECT);
    }

    private static String retrieve(String question, String answerState) throws IOException {
        var answers =
                Arrays.stream(
                                convert("KennethDecerqueira.json")
                                        .get(question.toUpperCase())
                                        .toString()
                                        .split(";"))
                        .map(String::toLowerCase)
                        .filter(item -> item.contains(answerState))
                        .collect(Collectors.toList())
                        .stream()
                        .collect(Collectors.joining());
        return answers.split(",")[answers.length() == 2 ? 1 : 0];
    }

    private static Map<String, Object> convert(String resourceName) throws IOException {
        InputStream inputStream =
                AnswerResource.class.getClassLoader().getResourceAsStream(resourceName);
        System.out.println("inputStream = " + inputStream);
        Reader reader = new InputStreamReader(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(reader, Map.class);

        return map;
    }
}
