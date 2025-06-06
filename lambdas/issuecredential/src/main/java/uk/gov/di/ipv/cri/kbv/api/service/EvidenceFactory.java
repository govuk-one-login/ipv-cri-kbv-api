package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.builder.CheckDetailsBuilder;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.BuildTo;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_UNABLE_TO_AUTHENTICATE;

public class EvidenceFactory {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
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

    public Object[] create(KBVItem kbvItem, EvidenceRequest evidenceRequest)
            throws JsonProcessingException {
        String kbvStatus = kbvItem.getStatus();
        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());
        KbvQuestionAnswerSummary summary = kbvItem.getQuestionAnswerResultSummary();
        QuestionState questionState =
                objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);

        if (hasQuestionsAsked(summary)) {
            evidence.setCheckDetails(withKbvQuality(questionState, summary));
            evidence.setFailedCheckDetails(withoutKbvQuality(summary));
        }
        if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(evidenceRequest);
            logVcScore("pass");
        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            logVcScore("fail");
            if (hasTooManyIncorrectAnswers(kbvStatus, summary)) {
                evidence.setCi(new ContraIndicator[] {ContraIndicator.V03});
            }
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }

    /**
     * Return CheckDetail[] with CheckDetail elements that have kbvQuality values assigned according
     * to the following scenarios:
     *
     * <p>Scenario: It completed in 3 batches of requests, the (1st batch of 2 questions) were
     * answered correctly, the answer to the question in the (second batch of 1 question) was
     * incorrect and the answer to the question in the (3rd batch of 1 question) was correct. Since
     * 1 question was returned in the 2nd batch, we can infer it was wrong and exclude it from the
     * CheckDetail and correctly assign kbvQuality using the correct questions respectively.
     *
     * <p>Scenario: It completed 2 batches of requests, one of the questions in the (1st batch of 2
     * questions) was incorrect; we don't know which is wrong, the final batch (of 2 questions) were
     * answered correctly. To assign Kbv Quality we sort and eliminate the highest. creating
     * CheckDetails with the 3 lowest kbvQuality values
     *
     * <p>Scenario: All questions are answered correctly, none was incorrect. For 3 out of 4
     * strategy, this means (3 out of 3) success completion in 2 batches (1st batch with 2
     * questions) and (2nd batch with 1 question) For 2 out of 3 strategy, this means (2 out of 2)
     * success completion in a single batch (i.e 2 question was answered correct, no need for
     * another) CheckDetails are assigned with their respective kbvQuality
     *
     * @param questionState
     * @param summary of the outcome of answered question number asked, numbers incorrect or numbers
     *     correct
     * @return A array CheckDetails, with KbvQuality assigned to each
     */
    private CheckDetail[] withKbvQuality(
            QuestionState questionState, KbvQuestionAnswerSummary summary) {
        BuildTo checkDetails;
        int batch = questionState.getBatchCount();
        int correctAnswers = summary.getAnsweredCorrect();
        List<List<QuestionAnswerPair>> allQaBatches = questionState.getAllQaPairs();
        var builder = new CheckDetailsBuilder(allQaBatches, correctAnswers, kbvQualityMapping);

        if (batchNumberOfIncorrectAnswer(summary, batch) == 3) {
            checkDetails =
                    builder.skip1stQuestionIdIn2ndBatch()
                            .getUnSkippedQuestionIdsInBatches()
                            .createCheckDetailsWithKbvQuality();
        } else if (batchNumberOfIncorrectAnswer(summary, batch) == 2) {
            checkDetails =
                    builder.getQuestionIdsInBatches()
                            .createCheckDetailsWithKbvQuality()
                            .sortByKbvQualityFromLowestToHighest()
                            .filterByNumberOfCorrectQuestions();
        } else if (twoCorrectIn1Batch(batch, summary) || threeCorrectIn2Batches(batch, summary)) {
            checkDetails = builder.getQuestionIdsInBatches().createCheckDetailsWithKbvQuality();
        } else {
            checkDetails =
                    builder.getQuestionIdsInBatches()
                            .createCheckDetailsWithKbvQuality()
                            .filterByNumberOfCorrectQuestions();
        }
        return checkDetails.buildToArray();
    }

    private CheckDetail[] withoutKbvQuality(KbvQuestionAnswerSummary summary) {
        return IntStream.range(0, summary.getAnsweredIncorrect())
                .mapToObj(i -> new CheckDetail())
                .toArray(CheckDetail[]::new);
    }

    private boolean hasQuestionsAsked(KbvQuestionAnswerSummary summary) {
        return Objects.nonNull(summary) && summary.getQuestionsAsked() > 0;
    }

    private boolean twoCorrectIn1Batch(int batchCount, KbvQuestionAnswerSummary summary) {
        return batchCount == 1 && hasPassed(summary, 2, 2);
    }

    private boolean threeCorrectIn2Batches(int batchCount, KbvQuestionAnswerSummary summary) {
        return batchCount == 2 && hasPassed(summary, 3, 3);
    }

    private int batchNumberOfIncorrectAnswer(KbvQuestionAnswerSummary summary, int batchCount) {
        return hasPassedWithOneIncorrectAnswer(summary, batchCount) ? batchCount : -1;
    }

    private boolean hasPassedWithOneIncorrectAnswer(KbvQuestionAnswerSummary summary, int batch) {
        return Objects.nonNull(summary)
                && batch > 1
                && summary.getQuestionsAsked() - summary.getAnsweredCorrect() == 1;
    }

    private boolean hasPassed(KbvQuestionAnswerSummary summary, int min, int max) {
        return Objects.nonNull(summary)
                && summary.getQuestionsAsked() == max
                && summary.getAnsweredCorrect() == min;
    }

    private boolean hasTooManyIncorrectAnswers(String status, KbvQuestionAnswerSummary summary) {
        return Objects.nonNull(summary)
                && ((VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED.equalsIgnoreCase(status)
                                && summary.getAnsweredIncorrect() > 1)
                        || (VC_THIRD_PARTY_KBV_CHECK_UNABLE_TO_AUTHENTICATE.equalsIgnoreCase(status)
                                && summary.getAnsweredIncorrect() > 0));
    }

    private void logVcScore(String result) {
        eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, result));

        LOGGER.info("kbv {}", result);
    }
}
