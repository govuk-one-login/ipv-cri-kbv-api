package uk.gov.di.ipv.cri.kbv.api.util;

import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;

import java.util.Objects;

public class EvidenceUtils {
    private static final int VC_PASS_EVIDENCE_SCORE = 2;

    public static int getVerificationScoreForPass(EvidenceRequest evidenceRequest) {
        if (Objects.isNull(evidenceRequest)) {
            return VC_PASS_EVIDENCE_SCORE;
        }
        int verificationScore = evidenceRequest.getVerificationScore();
        return verificationScore > 0 ? verificationScore : VC_PASS_EVIDENCE_SCORE;
    }
}
