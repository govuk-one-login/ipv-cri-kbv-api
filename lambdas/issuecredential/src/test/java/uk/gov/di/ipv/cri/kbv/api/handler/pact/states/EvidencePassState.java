package uk.gov.di.ipv.cri.kbv.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface EvidencePassState {
    @State("VC evidence verificationScore is 2")
    default void verificationScore() {}

    @State("VC evidence txn is dummyTxn")
    default void txn() {}

    @State("VC evidence checkDetails kbvQuality are 2, 2 and 1")
    default void checkDetailsKbvQualityValue() {}

    @State("VC evidence checkDetails are multiple_choice, multiple_choice, multiple_choice")
    default void checkDetailsMultiChoice() {}
}
