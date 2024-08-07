package uk.gov.di.ipv.cri.kbv.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface EvidenceCIState {
    @State("VC evidence verificationScore is 0")
    default void verificationScore() {}

    @State("VC evidence txn is dummyTxn")
    default void txn() {}

    @State("VC evidence checkDetails are multiple_choice")
    default void checkDetailsMultiChoice() {}

    @State("VC evidence checkDetails kbvQuality are 3")
    default void checkDetailsKbvQualityValue() {}

    @State("VC evidence failedCheckDetails are multiple_choice, multiple_choice multiple_choice")
    default void failCheckDetailsMultiChoice() {}

    @State("VC ci is V03")
    default void contraIndicatorValue() {}
}
