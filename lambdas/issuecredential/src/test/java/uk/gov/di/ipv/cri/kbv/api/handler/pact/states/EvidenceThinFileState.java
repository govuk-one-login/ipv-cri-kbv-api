package uk.gov.di.ipv.cri.kbv.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface EvidenceThinFileState {
    @State("VC evidence verificationScore is 0")
    default void verificationScore() {}

    @State("VC evidence txn is dummyTxn")
    default void txn() {}
}
