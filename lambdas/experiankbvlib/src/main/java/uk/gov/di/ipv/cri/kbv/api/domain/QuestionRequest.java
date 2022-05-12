package uk.gov.di.ipv.cri.kbv.api.domain;

public class QuestionRequest {
    private String urn;

    private String strategy;

    private PersonIdentity personIdentity;

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public void setPersonIdentity(PersonIdentity personIdentity) {
        this.personIdentity = personIdentity;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getUrn() {
        return urn;
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }
}
