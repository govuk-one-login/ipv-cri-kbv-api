package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionOptions;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;

import java.util.List;
import java.util.Objects;

class QuestionsResponseMapper {
    QuestionsResponse mapRTQResponse(RTQResponse2 response) {
        QuestionsResponse questionsResponse = new QuestionsResponse();

        questionsResponse.setQuestions(mapQuestions(response.getQuestions()));

        questionsResponse.setResults(mapResults(response.getResults()));

        mapControl(questionsResponse, response.getControl());

        mapError(questionsResponse, response.getError());

        return questionsResponse;
    }

    QuestionsResponse mapSAAResponse(SAAResponse2 sAAResponse2) {
        QuestionsResponse questionsResponse = new QuestionsResponse();

        questionsResponse.setQuestions(mapQuestions(sAAResponse2.getQuestions()));

        mapControl(questionsResponse, sAAResponse2.getControl());

        mapError(questionsResponse, sAAResponse2.getError());

        questionsResponse.setResults(mapResults(sAAResponse2.getResults()));

        return questionsResponse;
    }

    private KbvQuestion[] mapQuestions(Questions sourceQuestions) {
        if (Objects.nonNull(sourceQuestions) && notNullAndNotEmpty(sourceQuestions.getQuestion())) {
            return sourceQuestions.getQuestion().stream()
                    .map(
                            sourceQuestion -> {
                                KbvQuestion kbvQuestion = new KbvQuestion();
                                kbvQuestion.setQuestionId(sourceQuestion.getQuestionID());
                                kbvQuestion.setTooltip(sourceQuestion.getTooltip());
                                kbvQuestion.setText(sourceQuestion.getText());
                                if (Objects.nonNull(sourceQuestion.getAnswerFormat())) {
                                    KbvQuestionOptions kbvQuestionOptions =
                                            new KbvQuestionOptions();
                                    kbvQuestionOptions.setFieldType(
                                            sourceQuestion.getAnswerFormat().getFieldType());
                                    kbvQuestionOptions.setIdentifier(
                                            sourceQuestion.getAnswerFormat().getIdentifier());
                                    kbvQuestionOptions.setOptions(
                                            sourceQuestion.getAnswerFormat().getAnswerList());
                                    kbvQuestion.setQuestionOptions(kbvQuestionOptions);
                                }
                                return kbvQuestion;
                            })
                    .toArray(KbvQuestion[]::new);
        }
        return new KbvQuestion[0];
    }

    private KbvResult mapResults(Results results) {
        KbvResult kbvResult = null;
        if (Objects.nonNull(results)) {
            kbvResult = new KbvResult();
            kbvResult.setAuthenticationResult(results.getAuthenticationResult());
            kbvResult.setConfirmationCode(results.getConfirmationCode());
            kbvResult.setNextTransId(
                    Objects.nonNull(results.getNextTransId())
                            ? results.getNextTransId().getString().toArray(String[]::new)
                            : null);
            kbvResult.setOutcome(results.getOutcome());
            if (Objects.nonNull(results.getQuestions())) {
                KbvQuestionAnswerSummary summary = new KbvQuestionAnswerSummary();
                summary.setAnsweredCorrect(results.getQuestions().getCorrect());
                summary.setAnsweredIncorrect(results.getQuestions().getIncorrect());
                summary.setQuestionsAsked(results.getQuestions().getAsked());
                kbvResult.setAnswerSummary(summary);
            }
        }
        return kbvResult;
    }

    private void mapError(QuestionsResponse questionsResponse, Error error) {
        if (Objects.nonNull(error)) {
            questionsResponse.setErrorCode(error.getErrorCode());
            questionsResponse.setErrorMessage(error.getMessage());
        }
    }

    private void mapControl(QuestionsResponse questionsResponse, Control control) {
        if (Objects.nonNull(control)) {
            questionsResponse.setAuthReference(control.getAuthRefNo());
            questionsResponse.setUniqueReference(control.getURN());
        }
    }

    private <T> boolean notNullAndNotEmpty(List<T> items) {
        return Objects.nonNull(items) && !items.isEmpty();
    }
}
