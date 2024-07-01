package uk.gov.di.ipv.cri.kbv.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceUtilsTest {
    @Test
    @DisplayName("Should return pass verification score of 2 by default when given null")
    void returnsPassVerificationScoreOfTwoByDefaultWhenGivenNull() {
        assertEquals(2, EvidenceUtils.getVerificationScoreForPass(null));
    }

    @Test
    @DisplayName("Should return pass verification score of 2 by default when given zero")
    void returnsPassVerificationScoreOfTwoByDefaultWhenGivenZero() {
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(0);
        assertEquals(2, EvidenceUtils.getVerificationScoreForPass(evidenceRequest));
    }

    @ParameterizedTest
    @CsvSource({"3", "100", "-2", "-4"})
    @DisplayName("Should throw IllegalStateException when given any value greater than 2")
    void throwsIllegalStateExceptionWhenGivenAnyValueGreaterThanTwo(int score) {
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(score);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> EvidenceUtils.getVerificationScoreForPass(evidenceRequest));

        assertEquals(
                String.format("Verification Score %d is not supported", score),
                exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"1", "2"})
    @DisplayName("Should return verification score when given a value of 1 or 2")
    void returnsVerificationScoreWhenGivenAValueOfOneOrTwo(int score) {
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(score);

        assertEquals(score, EvidenceUtils.getVerificationScoreForPass(evidenceRequest));
    }
}
