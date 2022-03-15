package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.di.ipv.cri.kbv.api.domain.Question;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.helper.ApiGatewayResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class QuestionService extends ApiGatewayResponse {
    private ExperianService experianService;
    private StorageService storageService;

    public boolean respondWithQuestionFromDbStore(QuestionState questionState)
            throws JsonProcessingException {
        // TODO Handle scenario when no questions are available
        Optional<Question> nextQuestion = questionState.getNextQuestion();
        if (nextQuestion.isPresent()) {
            ok(nextQuestion.get());
            return true;
        }
        return false;
    }

    public void respondWithQuestionFromExperianThenStoreInDb(
            String json, KBVSessionItem kbvSessionItem, QuestionState questionState)
            throws IOException, InterruptedException {
        // we should fall in this block once only
        // fetch a batch of questions from experian kbv wrapper
        if (kbvSessionItem.getAuthorizationCode() != null) {
            noContent();
            return;
        }
        String body =
                experianService.getResponseFromKBVExperianAPI(
                        json,
                        kbvSessionItem.getAuthRefNo() == null
                                ? "EXPERIAN_API_WRAPPER_SAA_RESOURCE"
                                : "EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        QuestionsResponse questionsResponse = readValue(body, QuestionsResponse.class);
        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            questionState.setState(questionsResponse.getQuestionStatus());
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            ok(nextQuestion.get());

            String state = generateResponseBody(questionState);
            kbvSessionItem.setQuestionState(state);
            kbvSessionItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
            kbvSessionItem.setUrn(questionsResponse.getControl().getURN());
            storageService.update(kbvSessionItem);
        } else { // TODO: Alternate flow when first request does not return questions
            badRequest(body);
        }
    }

    public boolean respondWithAnswerFromDbStore(
            QuestionAnswer answer, QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws JsonProcessingException {

        questionState.setAnswer(answer);
        kbvSessionItem.setQuestionState(generateResponseBody(questionState));
        storageService.update(kbvSessionItem);
        ok("");

        return questionState.hasAtLeastOneUnAnswered();
    }

    public void respondWithAnswerFromExperianThenStoreInDb(
            String json, QuestionState questionState, KBVSessionItem kbvSessionItem)
            throws IOException, InterruptedException {

        String body =
                experianService.getResponseFromKBVExperianAPI(
                        json, "EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        QuestionsResponse questionsResponse = readValue(body, QuestionsResponse.class);

        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            questionState.setState(questionsResponse.getQuestionStatus());
            kbvSessionItem.setQuestionState(generateResponseBody(questionState));
            storageService.update(kbvSessionItem);
        } else if (questionsResponse.hasQuestionRequestEnded()) {
            kbvSessionItem.setQuestionState(generateResponseBody(questionState));
            kbvSessionItem.setAuthorizationCode(UUID.randomUUID().toString());
            kbvSessionItem.setStatus(questionsResponse.getStatus());
            storageService.update(kbvSessionItem);
        } else {
            // TODO: alternate flow could end of transaction / or others
        }
        ok("");
    }
}
