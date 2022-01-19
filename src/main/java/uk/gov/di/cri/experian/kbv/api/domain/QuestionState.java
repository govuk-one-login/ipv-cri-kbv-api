package uk.gov.di.cri.experian.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Question;

public class QuestionState {

    private PersonIdentity personIdentity;

    private QuestionsResponse questionsResponse;

    public QuestionState(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }

    public void setQuestionsResponse(QuestionsResponse questionsResponse) {
        this.questionsResponse = questionsResponse;
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }

    public QuestionsResponse getQuestionsResponse() {
        return questionsResponse;
    }

    public Question getNextQuestion() {
        return questionsResponse.getQuestions().getQuestion().iterator().next();
    }
}
