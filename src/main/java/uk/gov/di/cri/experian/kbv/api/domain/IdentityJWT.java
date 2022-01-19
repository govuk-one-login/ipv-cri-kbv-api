package uk.gov.di.cri.experian.kbv.api.domain;

public class IdentityJWT {

    private final String urn;
    private final PersonIdentity personIdentity;

    public IdentityJWT(String urn, PersonIdentity personIdentity) {
        this.urn = urn;
        this.personIdentity = personIdentity;
    }

    public String getUrn() {
        return urn;
    }

    public PersonIdentity getPersonIdentity() {
        return personIdentity;
    }
}
