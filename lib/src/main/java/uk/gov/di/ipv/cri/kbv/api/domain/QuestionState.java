package uk.gov.di.ipv.cri.kbv.api.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionState {

    private Control control;
    private Integer skipsRemaining;
    private String skipWarning;
    private List<QuestionAnswerPair> qaPairs = new ArrayList<>();
    private NextQuestion nextQuestion;

    public QuestionState() {}

    public boolean setQuestionsResponse(QuestionsResponse questionsResponse) {
        setControl(questionsResponse.getControl());
        Questions questions = questionsResponse.getQuestions();
        boolean hasQuestions = questions != null && questions.getQuestion() != null;
        if (hasQuestions) {
            skipsRemaining = questions.getSkipsRemaining();
            skipWarning = questions.getSkipWarning();

            qaPairs =
                    Arrays.stream(questions.getQuestion())
                            .map(question -> new QuestionAnswerPair(question))
                            .collect(Collectors.toList());
        }
        return hasQuestions;
    }

    public List<QuestionAnswerPair> getQaPairs() {
        return Collections.unmodifiableList(qaPairs);
    }

    public Optional<Question> getNextQuestion() {
        return qaPairs.stream()
                .filter(pair -> pair.getAnswer() == null)
                .map(pair -> pair.getQuestion())
                .findFirst();
    }

    public void setControl(Control control) {
        this.control = control;
    }

    public Control getControl() {
        return control;
    }

    public void setAnswer(QuestionAnswer answer) {

        QuestionAnswerPair questionAnswerPair =
                qaPairs.stream()
                        .filter(
                                pair ->
                                        pair.getQuestion()
                                                .getQuestionID()
                                                .equals(answer.getQuestionId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "question not found for "
                                                        + answer.getQuestionId()));
        questionAnswerPair.setAnswer(answer.getAnswer());
    }

    public boolean canSubmitAnswers() {
        return qaPairs.stream().allMatch(qa -> qa.getAnswer() != null);
    }
}
