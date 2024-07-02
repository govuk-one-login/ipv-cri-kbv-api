package uk.gov.di.ipv.cri.kbv.api.builder;

import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuality;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestionAnswerSummary;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerPair;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;

public class CheckDetailsBuilder {
    private static final int UNSUITABLE_QUESTION_QUALITY = 0;
    private static QuestionState questionState;
    private static KbvQuestionAnswerSummary answerSummary;
    private static Map<String, Integer> kbvQualityMapping;
    private List<List<QuestionAnswerPair>> qaBatches;
    private Stream<List<QuestionAnswerPair>> batchWithExclusion;
    private Stream<CheckDetail> checkDetailStream;
    private Stream<String> qaIds;

    public static CheckDetailsBuilder init(
            QuestionState questionState,
            KbvQuestionAnswerSummary answerSummary,
            Map<String, Integer> kbvQualityMapping) {
        CheckDetailsBuilder.kbvQualityMapping = kbvQualityMapping;
        CheckDetailsBuilder.answerSummary = answerSummary;
        CheckDetailsBuilder.questionState = questionState;

        return new CheckDetailsBuilder(questionState);
    }

    public CheckDetailsBuilder sortByKbvQualityFromLowestToHighest() {
        checkDetailStream = checkDetailStream.sorted(comparingInt(CheckDetail::getKbvQuality));
        return this;
    }

    public CheckDetailsBuilder filterByNumberOfCorrectQuestions() {
        checkDetailStream = checkDetailStream.limit(answerSummary.getAnsweredCorrect());
        return this;
    }

    public CheckDetailsBuilder createCheckDetailsWithKbvQuality() {
        checkDetailStream = qaIds.map(this::createCheckDetailWithQuality);
        return this;
    }

    public CheckDetailsBuilder getAllBatchesQuestionIds() {
        qaIds =
                qaBatches.stream()
                        .flatMap(List::stream)
                        .map(QuestionAnswerPair::getQuestion)
                        .map(KbvQuestion::getQuestionId);
        return this;
    }

    public CheckDetailsBuilder getFilteredBatchesQuestionIds() {
        qaIds =
                batchWithExclusion
                        .flatMap(List::stream)
                        .map(QuestionAnswerPair::getQuestion)
                        .map(KbvQuestion::getQuestionId);
        return this;
    }

    public CheckDetailsBuilder skip1stQuestionOfBatchTwo() {
        batchWithExclusion =
                qaBatches.stream()
                        .filter(Predicate.not(q -> questionState.getAllQaPairs().indexOf(q) == 1));
        return this;
    }

    public CheckDetail[] toArray() {
        return checkDetailStream.collect(Collectors.toList()).toArray(CheckDetail[]::new);
    }

    private static int mapToKbvQuality(String questionId) {
        return kbvQualityMapping.entrySet().stream()
                .filter(item -> questionId.equals(item.getKey()))
                .map(Map.Entry::getValue)
                .map(CheckDetailsBuilder::mapKbvQuality)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        String.format(
                                                "QuestionId: %s may not be present in Mapping",
                                                questionId)));
    }

    private static int mapKbvQuality(int quality) {
        return quality == UNSUITABLE_QUESTION_QUALITY ? KbvQuality.LOW.getValue() : quality;
    }

    private CheckDetailsBuilder(QuestionState questionState) {
        qaBatches = questionState.getAllQaPairs();
    }

    private CheckDetail createCheckDetailWithQuality(String questionId) {
        CheckDetail checkDetail = new CheckDetail();
        checkDetail.setKbvQuality(mapToKbvQuality(questionId));
        return checkDetail;
    }
}
