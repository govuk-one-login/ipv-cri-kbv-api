package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.builder.CheckDetailsBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.strategy.KbvStrategyParser;
import uk.gov.di.ipv.cri.kbv.api.strategy.Strategy;

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
    private String kbvQuestionStrategy;
    private final Map<String, Integer> kbvQualityMapping;

    void setKbvQuestionStrategy(String questionStrategy) {
        this.kbvQuestionStrategy = questionStrategy;
    }

    public String getKbvQuestionStrategy() {
        return this.kbvQuestionStrategy;
    }

    public Map<String, Integer> getKbvQualityMapping() {
        return kbvQualityMapping;
    }

    public EvidenceFactory(
            ObjectMapper objectMapper,
            EventProbe eventProbe,
            Map<String, Integer> kbvQualityMapping) {
        this.objectMapper = objectMapper;
        this.eventProbe = eventProbe;
        this.kbvQualityMapping = kbvQualityMapping;
        this.kbvQuestionStrategy = "3 out of 4 Prioritised";
    }

    public Object[] create(KBVItem kbvItem, EvidenceRequest evidenceRequest)
            throws JsonProcessingException {
        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());
        if (hasQuestionsAsked(kbvItem)) {
            evidence.setCheckDetails(withKbvQuality(kbvItem));
            evidence.setFailedCheckDetails(withoutKbvQuality(kbvItem));
        }
        if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(evidenceRequest);
            logVcScore("pass");
        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            logVcScore("fail");
            if (hasTooManyIncorrectAnswers(kbvItem)) {
                evidence.setCi(new ContraIndicator[] {ContraIndicator.V03});
            }
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }

    private CheckDetail[] withKbvQuality(KBVItem kbvItem) throws JsonProcessingException {
        QuestionState questionState =
                objectMapper.readValue(kbvItem.getQuestionState(), QuestionState.class);
        KbvQuestionAnswerSummary answerSummary = kbvItem.getQuestionAnswerResultSummary();

        if (batchNumberOfIncorrectAnswer(kbvItem, questionState) == 3) {
            /**
             * Scenario: 2 questions in first batch were correct, third question in the second batch
             * was incorrect, and fourth question in the third batch was correct. We can safely
             * exclude the third question from the `checkDetails`
             */
            return CheckDetailsBuilder.init(questionState, answerSummary, this.kbvQualityMapping)
                    .skip1stQuestionOfBatchTwo()
                    .getFilteredBatchesQuestionIds()
                    .createCheckDetailsWithKbvQuality()
                    .toArray();
        } else if (batchNumberOfIncorrectAnswer(kbvItem, questionState) == 2) {
            /**
             * Scenario: We create the `checkDetails` with the lowest 3 kbvQuality values, so we are
             * excluding the highest kbvQuality value.
             *
             * <p>(3 out of 4 prioritised) One question was wrong in the first batch, therefore 2
             * questions were given in second (2 out of 3 prioritised) One question was wrong in the
             * first batch, therefore 1 question was given in second batch, and they were answered
             * correctly. We can sort by quality and remove any additional the one the highest KBV
             * Quality value
             */
            return CheckDetailsBuilder.init(questionState, answerSummary, this.kbvQualityMapping)
                    .getAllBatchesQuestionIds()
                    .createCheckDetailsWithKbvQuality()
                    .sortByKbvQualityFromLowestToHighest()
                    .filterByNumberOfCorrectQuestions()
                    .toArray();
        } else if (twoCorrectInOneBatch(questionState) || threeCorrectInTwoBatches(questionState)) {
            /**
             * Scenario: 3 out of 3 correct answers from 2 batches (3 out of 4 prioritised) Or 2 out
             * of 2 correct answers from 1 batch (2 out of 3 prioritised)
             */
            return CheckDetailsBuilder.init(questionState, answerSummary, this.kbvQualityMapping)
                    .getAllBatchesQuestionIds()
                    .createCheckDetailsWithKbvQuality()
                    .filterByNumberOfCorrectQuestions()
                    .toArray();
        }
        return new CheckDetail[0];
    }

    private CheckDetail[] withoutKbvQuality(KBVItem kbvItem) {
        return IntStream.range(0, kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect())
                .mapToObj(i -> new CheckDetail())
                .limit(kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect())
                .toArray(CheckDetail[]::new);
    }

    private boolean hasQuestionsAsked(KBVItem kbvItem) {
        return Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked() > 0;
    }

    private boolean twoCorrectInOneBatch(QuestionState questionState) {
        return questionState.getBatchCount() == 1;
    }

    private boolean threeCorrectInTwoBatches(QuestionState questionState) {
        return questionState.getBatchCount() == 2;
    }

    private int batchNumberOfIncorrectAnswer(KBVItem kbvItem, QuestionState questionState) {
        return hasPassedWithOneIncorrectAnswer(kbvItem) ? questionState.getBatchCount() : -1;
    }

    private boolean hasPassedWithOneIncorrectAnswer(KBVItem kbvItem) {
        KbvStrategyParser parser = new KbvStrategyParser(this.getKbvQuestionStrategy());
        Strategy strategy = parser.parse();
        return Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getQuestionsAsked() == strategy.max()
                && kbvItem.getQuestionAnswerResultSummary().getAnsweredCorrect() == strategy.min();
    }

    private boolean hasTooManyIncorrectAnswers(KBVItem kbvItem) {
        var status = kbvItem.getStatus();
        var summary = kbvItem.getQuestionAnswerResultSummary();
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
