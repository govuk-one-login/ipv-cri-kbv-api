package uk.gov.di.ipv.cri.kbv.api.domain;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KbvResponsesAuditExtension {
    public static final String EXPERIAN_IIQ_RESPONSE = "experianIiqResponse";

    @ExcludeFromGeneratedCoverageReport
    private KbvResponsesAuditExtension() {}

    public static Map<String, Object> createAuditEventExtensions(
            QuestionsResponse questionsResponse) {

        if (Objects.nonNull(questionsResponse)
                && Objects.nonNull(questionsResponse.getStatus())
                && Objects.nonNull(questionsResponse.getResults())) {

            return createAuditEventExtensions(
                    questionsResponse.getStatus(),
                    questionsResponse.getResults().getAnswerSummary());
        }
        return Collections.emptyMap();
    }

    public static Map<String, Object> createAuditEventExtensions(
            String status, KbvQuestionAnswerSummary questionSummary) {

        return Objects.nonNull((status))
                ? getKbvResponsesSummaryEntries(status, questionSummary)
                : Collections.emptyMap();
    }

    private static Map<String, Object> getKbvResponsesSummaryEntries(
            String status, KbvQuestionAnswerSummary questionSummary) {

        Map<String, Object> contextEntries = new HashMap<>();
        contextEntries.put("outcome", status);

        if (Objects.nonNull(questionSummary)) {
            contextEntries.put("totalQuestionsAsked", questionSummary.getQuestionsAsked());
            contextEntries.put(
                    "totalQuestionsAnsweredCorrect", questionSummary.getAnsweredCorrect());
            contextEntries.put(
                    "totalQuestionsAnsweredIncorrect", questionSummary.getAnsweredIncorrect());
        }
        return contextEntries;
    }
}
