package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionState {
    private List<QuestionAnswerPair> qaPairs = new ArrayList<>();

    public void setAnswer(QuestionAnswer questionAnswer) {
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
            setQAPairs(questionsResponse.getQuestions());
        }
        return hasQuestions;
    }

    public void setQAPairs(KbvQuestion[] questions) {
        this.qaPairs =
                Arrays.stream(questions).map(QuestionAnswerPair::new).collect(Collectors.toList());
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

    public boolean hasAtLeastOneUnAnswered() {
        return qaPairs.stream().anyMatch(qa -> Objects.isNull(qa.getAnswer()));
    }
}
