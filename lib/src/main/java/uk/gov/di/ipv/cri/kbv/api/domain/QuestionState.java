package uk.gov.di.ipv.cri.kbv.api.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class QuestionState {

    private PersonIdentity personIdentity;
    private Control control;
    private Integer skipsRemaining;
    private String skipWarning;
    private List<QuestionAnswerPair> qaPairs = new ArrayList<>();
    private NextQuestion nextQuestion;

    private QuestionState() {}

    public QuestionState(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }

    public boolean setQuestionsResponse(QuestionsResponse questionsResponse) {
        setControl(questionsResponse.getControl());

        Results results = questionsResponse.getResults();
        String outcome = results.getOutcome();
        System.out.println("outcome is " + outcome);
        String authenticationResult = results.getAuthenticationResult();
        System.out.println("authenticationResult is " + authenticationResult);
        NextTransId nextTransId = results.getNextTransId();
        System.out.println("transition is " + nextTransId);

        Questions questions = questionsResponse.getQuestions();

        boolean hasQuestions = questions != null && questions.getQuestion() != null;
        if (hasQuestions) {
            skipsRemaining = questions.getSkipsRemaining();
            skipWarning = questions.getSkipWarning();
            for (Question question : questions.getQuestion()) {
                qaPairs.add(new QuestionAnswerPair(question));
            }
        }

        return hasQuestions;
    }

    public List<QuestionAnswerPair> getQaPairs() {
        return Collections.unmodifiableList(qaPairs);
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
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

    public boolean submitAnswers() {
        return qaPairs.stream().allMatch(qa -> qa.getAnswer() != null);
    }

    public static class QuestionAnswerPair {

        private final Question question;
        private String answer;

        public QuestionAnswerPair(Question question) {
            this.question = question;
        }

        public Question getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
