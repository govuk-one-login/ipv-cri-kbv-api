package uk.gov.di.ipv.cri.kbv.api.domain;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KbvResponsesAuditExtension {
    public static final String EXPERIAN_IIQ_RESPONSE = "experianIiqResponse";

    @ExcludeFromGeneratedCoverageReport
    private KbvResponsesAuditExtension() {}

    public static Map<String, Object> createAuditEventExtensions(
            QuestionsResponse questionsResponse) {
        Map<String, Object> contextEntries = new HashMap<>();

        if (Objects.nonNull(questionsResponse) && Objects.nonNull(questionsResponse.getStatus())) {
            contextEntries.put("outcome", questionsResponse.getStatus());

            if (Objects.nonNull(questionsResponse.getResults())
                    && Objects.nonNull(questionsResponse.getResults().getAnswerSummary())) {
                KbvQuestionAnswerSummary answerSummary =
                        questionsResponse.getResults().getAnswerSummary();
                addQuestionSummaryData(contextEntries, answerSummary);
            }
        }

        return contextEntries;
    }

    private static void addQuestionSummaryData(
            Map<String, Object> contextEntries, KbvQuestionAnswerSummary questionSummary) {
        if (Objects.nonNull(questionSummary)) {
            contextEntries.put("totalQuestionsAsked", questionSummary.getQuestionsAsked());
            contextEntries.put(
                    "totalQuestionsAnsweredCorrect", questionSummary.getAnsweredCorrect());
            contextEntries.put(
                    "totalQuestionsAnsweredIncorrect", questionSummary.getAnsweredIncorrect());
        }
    }
}
