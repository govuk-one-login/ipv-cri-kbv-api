package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
                .map(Collection::stream)
                .flatMap(q -> q.map(QuestionAnswerPair::getQuestion))
                .map(KbvQuestion::getQuestionId);
    }

    @JsonIgnore
    public Stream<List<QuestionAnswerPair>> skipQaPairAtIndexOne() {
        return allQaPairs.stream().filter(Predicate.not(q -> allQaPairs.indexOf(q) == 1));
    }

    public void setAnswer(QuestionAnswer questionAnswer) {
        LOGGER.info("QAPair size is: {}", this.getQaPairs().size());
        LOGGER.info("AllQAPair size is: {}", this.getAllQaPairs().size());

        this.getQaPairs().stream()
                .filter(
                        pair ->
                                pair.getQuestion()
                                        .getQuestionId()
                                        .equals(questionAnswer.getQuestionId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Question not found for questionID: "
                                                + questionAnswer.getQuestionId()))
                .setAnswer(questionAnswer.getAnswer());
    }

    public boolean setQuestionsResponse(QuestionsResponse questionsResponse) {
        boolean hasQuestions = questionsResponse.hasQuestions();
        if (hasQuestions) {
            int qaPairSize = qaPairs.size();
            String questions = Arrays.toString(questionsResponse.getQuestions());
            LOGGER.info("setQuestionsResponse: QAPairs size: {}", qaPairSize);
            LOGGER.info("setQuestionsResponse: AllQAPairs size: {}", qaPairSize);

            LOGGER.info("KBVQuestion : {}", questions);
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
        int qaPairsSize = qaPairs.size();
        int questionLength = questions.length;
        LOGGER.info("QAPairs size: {}", qaPairsSize);
        LOGGER.info("AllQAPairs size: {}", qaPairsSize);

        LOGGER.info("KBVQuestion size: {}", questionLength);
        this.allQaPairs.add(
                Arrays.stream(questions).map(QuestionAnswerPair::new).collect(Collectors.toList()));
    }

    public List<QuestionAnswerPair> getQaPairs() {
        return this.qaPairs;
    }

    @JsonIgnore
    public List<QuestionAnswer> getAnswers() {
        return getQaPairs().stream()
                .map(
                        pair -> {
                            QuestionAnswer questionAnswer = new QuestionAnswer();
                            questionAnswer.setAnswer(pair.getAnswer());
                            questionAnswer.setQuestionId(pair.getQuestion().getQuestionId());
                            return questionAnswer;
                        })
                .collect(Collectors.toList());
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
}
