package uk.gov.di.ipv.cri.kbv.api.builder;

import uk.gov.di.ipv.cri.kbv.api.builder.steps.BuildTo;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.CreateCheckDetailsWithKbvQuality;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.Init;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.QuestionIdsInAllBatches;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.Skip1stQuestionIdIn2ndBatch;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.SortByKbvQuality;
import uk.gov.di.ipv.cri.kbv.api.builder.steps.UnSkippedQuestionIdsInAllBatches;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuality;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckDetailsBuilder {
    private static final int UNSUITABLE_QUESTION_QUALITY = 0;
    private final int numberOfCorrectAnswers;
    private final Map<String, Integer> kbvQualityMapping;
    private List<List<QuestionAnswerPair>> qaBatches;
    private Stream<List<QuestionAnswerPair>> skipFirstQuestionOf2ndBatch;
    private Stream<CheckDetail> checkDetailStream;
    private Stream<String> qaIds;

    public CheckDetailsBuilder(
            List<List<QuestionAnswerPair>> questionPairs,
            int numberOfCorrectAnswers,
            Map<String, Integer> kbvQualityMapping) {
        this.numberOfCorrectAnswers = numberOfCorrectAnswers;
        this.kbvQualityMapping = kbvQualityMapping;
        this.qaBatches = questionPairs;
    }

    public Skip1stQuestionIdIn2ndBatch skip1stQuestionIdIn2ndBatch() {
        return new BuilderSteps(this).skip1stQuestionIdIn2ndBatch();
    }

    public QuestionIdsInAllBatches getQuestionIdsInBatches() {
        return new BuilderSteps(this).getQuestionIdsInAllBatches();
    }

    private class BuilderSteps
            implements Init,
                    Skip1stQuestionIdIn2ndBatch,
                    QuestionIdsInAllBatches,
                    UnSkippedQuestionIdsInAllBatches,
                    CreateCheckDetailsWithKbvQuality,
                    SortByKbvQuality,
                    BuildTo {

        private final CheckDetailsBuilder builder;

        BuilderSteps(CheckDetailsBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Skip1stQuestionIdIn2ndBatch skip1stQuestionIdIn2ndBatch() {
            skipFirstQuestionOf2ndBatch =
                    qaBatches.stream().filter(Predicate.not(q -> qaBatches.indexOf(q) == 1));

            return this;
        }

        @Override
        public QuestionIdsInAllBatches getQuestionIdsInAllBatches() {
            qaIds =
                    qaBatches.stream()
                            .flatMap(List::stream)
                            .map(QuestionAnswerPair::getQuestion)
                            .map(KbvQuestion::getQuestionId);

            return this;
        }

        @Override
        public UnSkippedQuestionIdsInAllBatches getUnSkippedQuestionIdsInBatches() {
            qaIds =
                    skipFirstQuestionOf2ndBatch
                            .flatMap(List::stream)
                            .map(QuestionAnswerPair::getQuestion)
                            .map(KbvQuestion::getQuestionId);

            return this;
        }

        @Override
        public CreateCheckDetailsWithKbvQuality createCheckDetailsWithKbvQuality() {
            checkDetailStream = qaIds.map(builder::createCheckDetailWithQuality);

            return this;
        }

        @Override
        public SortByKbvQuality sortByKbvQualityFromLowestToHighest() {
            checkDetailStream =
                    checkDetailStream.sorted(Comparator.comparingInt(CheckDetail::getKbvQuality));

            return this;
        }

        @Override
        public BuildTo filterByNumberOfCorrectQuestions() {
            checkDetailStream = checkDetailStream.limit(numberOfCorrectAnswers);

            return this;
        }

        @Override
        public CheckDetail[] buildToArray() {
            return checkDetailStream.collect(Collectors.toList()).toArray(CheckDetail[]::new);
        }

        @Override
        public List<String> buildToList() {
            return qaIds.collect(Collectors.toList());
        }
    }

    private int mapToKbvQuality(String questionId) {
        return kbvQualityMapping.entrySet().stream()
                .filter(item -> questionId.equals(item.getKey()))
                .map(Map.Entry::getValue)
                .map(this::getKbvQuality)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "QuestionId: %s may not be present in Mapping",
                                                questionId)));
    }

    private int getKbvQuality(int quality) {
        return quality == UNSUITABLE_QUESTION_QUALITY ? KbvQuality.LOW.getValue() : quality;
    }

    private CheckDetail createCheckDetailWithQuality(String questionId) {
        CheckDetail checkDetail = new CheckDetail();
        checkDetail.setKbvQuality(this.mapToKbvQuality(questionId));

        return checkDetail;
    }
}
