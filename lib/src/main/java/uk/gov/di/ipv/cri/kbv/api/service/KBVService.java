package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KBVService {
    private final KBVGateway kbvGateway;

    public KBVService(KBVGateway kbvGateway) {
        this.kbvGateway = kbvGateway;
    }

    public QuestionsResponse getQuestions(QuestionRequest questionRequest) {
        return this.kbvGateway.getQuestions(questionRequest);
    }

    public QuestionsResponse submitAnswers(QuestionAnswerRequest answers) {
        return kbvGateway.submitAnswers(answers);
    }

    public Map<String, Object> createAuditEventExtensions(QuestionsResponse questionsResponse) {
        Map<String, Object> contextEntries = new HashMap<>();
        contextEntries.put("outcome", questionsResponse.getStatus());
        if (Objects.nonNull(questionsResponse.getResults())
                && Objects.nonNull(questionsResponse.getResults().getAnswerSummary())) {
            KbvQuestionAnswerSummary questionSummary =
                    questionsResponse.getResults().getAnswerSummary();
            contextEntries.put("totalQuestionsAsked", questionSummary.getQuestionsAsked());
            contextEntries.put(
                    "totalQuestionsAnsweredCorrect", questionSummary.getAnsweredCorrect());
            contextEntries.put(
                    "totalQuestionsAnsweredIncorrect", questionSummary.getAnsweredIncorrect());
        }
        return Map.of("experianIiqResponse", contextEntries);
    }
}
