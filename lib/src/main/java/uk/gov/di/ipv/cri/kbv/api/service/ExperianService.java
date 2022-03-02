package uk.gov.di.ipv.cri.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

public class ExperianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperianService.class);
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
    public static final String EXPERIAN_API_WRAPPER_URL = "EXPERIAN_API_WRAPPER_URL";

    public String getResponseFromExperianAPI(String payload, String uriEndpoint)
            throws IOException, InterruptedException {
        URI wrapperResourceURI = createExperianUri(uriEndpoint);
        HttpRequest httpReq =
                HttpRequest.newBuilder()
                        .uri(wrapperResourceURI)
                        .setHeader("Accept", RESPONSE_TYPE_APPLICATION_JSON)
                        .setHeader("Content-Type", RESPONSE_TYPE_APPLICATION_JSON)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> res = null;

        res = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

        LOGGER.info(
                String.format(
                        "getResponseFromExperianAPI response status code: %s", res.statusCode()));
        String body = res.body();
        LOGGER.info(String.format("getResponseFromExperianAPI response: %s", body));

        return body;
    }

    public URI createExperianUri(String uriEndpoint) {
        String baseURL = System.getenv(EXPERIAN_API_WRAPPER_URL);
        String resource = System.getenv(uriEndpoint);
        return URI.create(baseURL + resource);
    }

    public QuestionAnswerRequest prepareToSubmitAnswers(QuestionState questionState) {
        QuestionAnswerRequest questionAnswerRequest = new QuestionAnswerRequest();
        List<QuestionAnswerPair> pairs = questionState.getQaPairs();

        List<QuestionAnswer> collect =
                pairs.stream()
                        .map(
                                pair -> {
                                    QuestionAnswer questionAnswer = new QuestionAnswer();
                                    questionAnswer.setAnswer(pair.getAnswer());
                                    questionAnswer.setQuestionId(
                                            pair.getQuestion().getQuestionID());
                                    return questionAnswer;
                                })
                        .collect(Collectors.toList());

        questionAnswerRequest.setQuestionAnswers(collect);
        questionAnswerRequest.setAuthRefNo(questionState.getControl().getAuthRefNo());
        questionAnswerRequest.setUrn(questionState.getControl().getURN());
        return questionAnswerRequest;
    }
}
