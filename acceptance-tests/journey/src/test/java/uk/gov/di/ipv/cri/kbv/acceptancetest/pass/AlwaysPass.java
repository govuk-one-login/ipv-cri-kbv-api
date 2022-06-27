package uk.gov.di.ipv.cri.kbv.acceptancetest.pass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * when tests are run from root and the package "uk.gov.di.ipv.cri.kbv.acceptancetest.journey" is
 * excluded, then gradle errors with "No tests found for given includes:". Having this test in a
 * separate package in this module avoids this as a test (this one) is found.
 */
class AlwaysPass {
    @Test
    void alwaysPass() {
        assertTrue(true);
    }
}
