package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.util.Map;
import java.util.Objects;

import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;

public class EvidenceFactory {
    public static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
    private final EventProbe eventProbe;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ObjectMapper objectMapper;

    @ExcludeFromGeneratedCoverageReport
    public EvidenceFactory() {
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
        this.eventProbe = new EventProbe();
    }

    public EvidenceFactory(ObjectMapper objectMapper, EventProbe eventProbe) {
        this.objectMapper = objectMapper;
        this.eventProbe = eventProbe;
    }

    public Object[] create(KBVItem kbvItem) {
        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());
        if (hasMultipleIncorrectAnswers(kbvItem)) {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            evidence.setCi(new ContraIndicator[] {ContraIndicator.V03});
            logVcScore("fail");
        } else if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(VC_PASS_EVIDENCE_SCORE);
            logVcScore("pass");
        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            logVcScore("fail");
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }

    private boolean hasMultipleIncorrectAnswers(KBVItem kbvItem) {
        return VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED.equalsIgnoreCase(kbvItem.getStatus())
                && Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect() > 1;
    }

    private void logVcScore(String result) {
        eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, result));
        LOGGER.info("kbv {}", result);
    }
}
