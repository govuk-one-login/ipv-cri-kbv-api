package uk.gov.di.ipv.cri.kbv.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface DummyStates {
    @State("dummyApiKey is a valid api key")
    default void validDummyApiKey() {}

    @State("dummyExperianKbvComponentId is a valid issuer")
    default void validDummyExperianKbvComponent() {}

    @State("test-subject is a valid subject")
    default void validSubject() {}
}
