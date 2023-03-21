package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuality;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;

public class EvidenceFactory {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
    private static final int UNSUITABLE_QUESTION_QUALITY = 0;
    private final EventProbe eventProbe;
    private final ObjectMapper objectMapper;
    private final Map<String, Integer> kbvQualityMapping;

    public EvidenceFactory(
            ObjectMapper objectMapper,
            EventProbe eventProbe,
            Map<String, Integer> kbvQualityMapping) {
        this.objectMapper = objectMapper;
        this.eventProbe = eventProbe;
        this.kbvQualityMapping = kbvQualityMapping;
    }

    public Object[] create(KBVItem kbvItem) throws JsonProcessingException {
        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());
        if (hasQuestionsAsked(kbvItem)) {
            evidence.setCheckDetails(createCheckDetailsWithKbvQuality(kbvItem));
            evidence.setFailedCheckDetails(createFailedCheckDetails(kbvItem));
        }
        if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(VC_PASS_EVIDENCE_SCORE);
            logVcScore("pass");
        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            logVcScore("fail");
            if (hasMultipleIncorrectAnswers(kbvItem)) {
                evidence.setCi(new ContraIndicator[] {ContraIndicator.V03});
            }
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }

    private CheckDetail[] createCheckDetailsWithKbvQuality(KBVItem kbvItem)
            throws JsonProcessingException {
        var questionState = objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);

        if (hasPassedWithOneIncorrectAnswer(kbvItem)) {
            if (questionState.allQuestionBatchSizesMatch(2)) {
                return createCheckDetailsBySortingOnKbvQuality(kbvItem, questionState);
            }
            return createCheckDetailsBySkipping3rdIncorrectQa(questionState);
        }
        return createCheckDetails(kbvItem, questionState);
    }

    private CheckDetail[] createCheckDetails(KBVItem kbvItem, QuestionState questionState) {
        return mapKbvQualityToCheckDetail(questionState)
                .get()
                .limit(kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect())
                .toArray(CheckDetail[]::new);
    }

    private CheckDetail[] createCheckDetailsBySkipping3rdIncorrectQa(QuestionState questionState) {
        return questionState
                .skipQaPairAtIndexOne()
                .flatMap(Collection::stream)
                .map(QuestionAnswerPair::getQuestion)
                .map(KbvQuestion::getQuestionId)
                .map(this::createCheckDetailWithQuality)
                .toArray(CheckDetail[]::new);
    }

    private CheckDetail[] createCheckDetailsBySortingOnKbvQuality(
            KBVItem kbvItem, QuestionState questionState) {
        return mapKbvQualityToCheckDetail(questionState)
                .get()
                .sorted(comparingInt(CheckDetail::getKbvQuality))
                .limit(kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect())
                .collect(Collectors.toList())
                .toArray(CheckDetail[]::new);
    }

    private CheckDetail[] createFailedCheckDetails(KBVItem kbvItem) {
        return IntStream.range(0, kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect())
                .mapToObj(i -> new CheckDetail())
                .limit(kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect())
                .toArray(CheckDetail[]::new);
    }

    private CheckDetail createCheckDetailWithQuality(String questionId) {
        CheckDetail checkDetail = new CheckDetail();
        checkDetail.setKbvQuality(getKbvQuality(questionId));
        return checkDetail;
    }

    private int getKbvQuality(String questionId) {
        return kbvQualityMapping.entrySet().stream()
                .filter(item -> questionId.equals(item.getKey()))
                .map(Map.Entry::getValue)
                .map(this::mapKbvQuality)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "QuestionId: %s may not be present in Mapping",
                                                questionId)));
    }

    private int mapKbvQuality(int quality) {
        return quality == UNSUITABLE_QUESTION_QUALITY ? KbvQuality.LOW.getValue() : quality;
    }

    private Supplier<Stream<CheckDetail>> mapKbvQualityToCheckDetail(QuestionState questionState) {
        return () ->
                questionState.getQuestionIdsFromQAPairs().map(this::createCheckDetailWithQuality);
    }

    private boolean hasQuestionsAsked(KBVItem kbvItem) {
        return Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked() > 0;
    }

    private boolean hasPassedWithOneIncorrectAnswer(KBVItem kbvItem) {
        return Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked() == 4
                && kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect() == 3;
    }

    private boolean hasMultipleIncorrectAnswers(KBVItem kbvItem) {
        return VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED.equalsIgnoreCase(kbvItem.getStatus())
                && Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && (kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect() > 1 
                    || (kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect() == 1 
                        && kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked() == 3));
    }

    private void logVcScore(String result) {
        eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, result));
        LOGGER.info("kbv {}", result);
    }
}
