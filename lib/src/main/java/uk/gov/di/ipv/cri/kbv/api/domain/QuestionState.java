package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuestionState {
    private static final Logger LOGGER = LogManager.getLogger(QuestionState.class);
    private List<QuestionAnswerPair> qaPairs = new ArrayList<>();
    private List<List<QuestionAnswerPair>> allQaPairs = new ArrayList<>();

    @JsonIgnore
    public boolean allQuestionBatchSizesMatch(int expectedBatchSize) {
        return allQaPairs.stream().allMatch(x -> x.size() == expectedBatchSize);
    }

    @JsonIgnore
    public Stream<String> getQuestionIdsFromQAPairs() {
        return allQaPairs.stream()
                .flatMap(List::stream)
                .map(QuestionAnswerPair::getQuestion)
                .map(KbvQuestion::getQuestionId);
    }

    @JsonIgnore
    public Stream<List<QuestionAnswerPair>> skipQaPairAtIndexOne() {
        return allQaPairs.stream().filter(Predicate.not(q -> allQaPairs.indexOf(q) == 1));
    }

    @JsonIgnore
    public String getQaPairsIds() {
        return getQaPairs().stream()
                .map(pair -> pair.getQuestion().getQuestionId())
                .collect(Collectors.joining(","));
    }

    @JsonIgnore
    public String getAllQaPairsIds() {
        return getQuestionIdsFromQAPairs().collect(Collectors.joining(","));
    }

    public void setAnswer(QuestionAnswer questionAnswer) {
        getQuestionAnswerPair(questionAnswer)
                .ifPresentOrElse(
                        pair -> pair.setAnswer(questionAnswer.getAnswer()),
                        () -> handleQuestionAnswerResubmission(questionAnswer));
    }

    public boolean setQuestionsResponse(QuestionsResponse questionsResponse) {
        boolean hasQuestions = questionsResponse.hasQuestions();
        if (hasQuestions) {
            setQAPairs(questionsResponse.getQuestions());
        }
        return hasQuestions;
    }

    public List<List<QuestionAnswerPair>> getAllQaPairs() {
        return allQaPairs;
    }

    public void setQAPairs(KbvQuestion[] questions) {
        this.qaPairs =
                Arrays.stream(questions).map(QuestionAnswerPair::new).collect(Collectors.toList());
        this.allQaPairs.add(
                Arrays.stream(questions).map(QuestionAnswerPair::new).collect(Collectors.toList()));

        logSizeInfo(questions);
    }

    public List<QuestionAnswerPair> getQaPairs() {
        return this.qaPairs;
    }

    @JsonIgnore
    public List<QuestionAnswer> getAnswers() {
        return getQaPairs().stream().map(QuestionAnswer::new).collect(Collectors.toList());
    }

    @JsonIgnore
    public Optional<KbvQuestion> getNextQuestion() {
        return qaPairs.stream()
                .filter(pair -> pair.getAnswer() == null)
                .map(QuestionAnswerPair::getQuestion)
                .findFirst();
    }

    public boolean questionsHaveAllBeenAnswered() {
        return qaPairs.stream().allMatch(qa -> Objects.nonNull(qa.getAnswer()));
    }

    public boolean hasAtLeastOneUnanswered() {
        return qaPairs.stream().anyMatch(qa -> Objects.isNull(qa.getAnswer()));
    }

    private Optional<QuestionAnswerPair> getQuestionAnswerPair(QuestionAnswer questionAnswer) {
        return this.getQaPairs().stream()
                .filter(
                        pair ->
                                pair.getQuestion()
                                        .getQuestionId()
                                        .equals(questionAnswer.getQuestionId()))
                .findFirst();
    }

    private boolean isQuestionAnswerInAllQaPairs(QuestionAnswer questionAnswer) {
        return this.getQuestionIdsFromQAPairs()
                .anyMatch(id -> id.equals(questionAnswer.getQuestionId()));
    }

    private void handleQuestionAnswerResubmission(QuestionAnswer questionAnswer) {
        if (isQuestionAnswerInAllQaPairs(questionAnswer)) {
            logIdStatus(
                    questionAnswer.getQuestionId(),
                    "Answered Question: {}, QuestionIds existing in allQAPairs: {} but not in QAPairs: {}");
        } else {
            logIdStatus(
                    questionAnswer.getQuestionId(),
                    "Answered Question: {}, QuestionIds does not exist in both allQAPairs: {} and QAPairs: {}");
            throw new IllegalStateException(
                    "Question not found for questionID: " + questionAnswer.getQuestionId());
        }
    }

    private void logSizeInfo(KbvQuestion[] questions) {
        int allQaPairsSize =
                this.allQaPairs.stream().flatMap(List::stream).collect(Collectors.toList()).size();
        LOGGER.info("QAPairs size: {}", qaPairs.size());
        LOGGER.info("KBVQuestion size: {}", questions.length);
        LOGGER.info("AllQAPairs size: {}", allQaPairsSize);
    }

    private void logIdStatus(String answer, String message) {
        LOGGER.info(message, answer, getAllQaPairsIds(), getQaPairsIds());
    }
}
