package uk.gov.di.cri.experian.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionState {

    private PersonIdentity personIdentity;
    private Control control;
    private Integer skipsRemaining;
    private String skipWarning;
    private List<QuestionAnswerPair> qas = new ArrayList<>();

    public QuestionState(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }

    public void setQuestionsResponse(QuestionsResponse questionsResponse) {
        setControl(questionsResponse.getControl());

        Questions questions = questionsResponse.getQuestions();
        this.skipsRemaining = questions.getSkipsRemaining();
        this.skipWarning = questions.getSkipWarning();
        List<Question> question = questions.getQuestion(); // possibly 2 q's

        for (Question q : question) {
            qas.add(new QuestionAnswerPair(q));
        }


    }

    public List<QuestionAnswerPair> getQas() {
        return Collections.unmodifiableList(qas);
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }

    public Question getNextQuestion() {
        for (QuestionAnswerPair pair : qas) {
            if (pair.getAnswer() == null) {
                return pair.getQuestion();
            }
        }
        return null;
    }

    public void setControl(Control control) {
        this.control = control;
    }

    public Control getControl() {
        return control;
    }

    public void setAnswer(QuestionAnswer answer) {
        for (QuestionAnswerPair qa : qas) {
            if (qa.getQuestion().getQuestionID().equals(answer.getQuestionId())) {
                qa.setAnswer(answer.getAnswer());
            }
        }
    }

    public boolean submitAnswers() {
        return qas.stream().allMatch(qa -> qa.getAnswer() != null);
    }

    public static class QuestionAnswerPair {

        private Question question;
        private String answer;

        public QuestionAnswerPair(Question question) {
            this.question = question;
        }

        public Question getQuestion() {
            return question;
        }

        public void setQuestion(Question question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

}
