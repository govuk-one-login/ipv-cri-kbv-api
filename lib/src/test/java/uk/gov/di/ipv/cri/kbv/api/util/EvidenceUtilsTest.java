package uk.gov.di.ipv.cri.kbv.api.util;

import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceUtilsTest {

    @Test
    void shouldGiveScoreOfTwoWhenNull() {
        assertEquals(2, EvidenceUtils.getVerificationScoreForPass(null));
    }

    @Test
    void shouldGiveScoreOfTwoWhenZero() {
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(0);
        assertEquals(2, EvidenceUtils.getVerificationScoreForPass(evidenceRequest));
    }

    @Test
    void shouldGiveScoreWhenGivenScoreGreaterThan0() {
        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setVerificationScore(100);
        assertEquals(100, EvidenceUtils.getVerificationScoreForPass(evidenceRequest));
    }
}
