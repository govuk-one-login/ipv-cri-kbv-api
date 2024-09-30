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
            String status, KbvQuestionAnswerSummary questionSummary) {
        return getKbvResponsesSummaryEntries(status, questionSummary);
    }

    public static Map<String, Object> createAuditEventExtensions(
            QuestionsResponse questionsResponse) {
        if (Objects.isNull(questionsResponse) || Objects.isNull(questionsResponse.getResults())) {
            return Collections.emptyMap();
        }
        return createAuditEventExtensions(
                questionsResponse.getStatus(), questionsResponse.getResults().getAnswerSummary());
    }

    public static Map<String, Object> createResponseReceivedAuditEventExtensions(
            QuestionsResponse questionsResponse) {
        Map<String, Object> extensionsMap = createAuditEventExtensions(questionsResponse);
        if (questionsResponse.isRepeatAttemptAlert()) {
            extensionsMap.put("repeatAttemptAlert", true);
        }
        return extensionsMap;
    }

    private static Map<String, Object> getKbvResponsesSummaryEntries(
            String status, KbvQuestionAnswerSummary questionSummary) {

        Map<String, Object> contextEntries = new HashMap<>();

        if (Objects.nonNull(status)) {
            contextEntries.put("outcome", status);
        }

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
