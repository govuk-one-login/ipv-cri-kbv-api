package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuality;
import uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.cri.kbv.api.service.EvidenceFactory.METRIC_DIMENSION_KBV_VERIFICATION;

@ExtendWith(MockitoExtension.class)
class EvidenceFactoryTest implements TestFixtures {
    private EvidenceFactory evidenceFactory;
    @Mock private EventProbe mockEventProbe;
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        evidenceFactory = new EvidenceFactory(objectMapper, mockEventProbe);
    }

    @Test
    void shouldPassWhenKbvItemStatusIsAuthenticated() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));

        doNothing()
                .when(mockEventProbe)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

        var result = evidenceFactory.create(kbvItem);

        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
        assertEquals(
                VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE,
                getEvidenceAsMap(result).get("verificationScore"));
        assertNull(getEvidenceAsMap(result).get("ci"));
    }

    @Test
    void shouldFailAndReturnContraIndicatorWhenMultipleAnswersAreIncorrect() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("not Authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));

        doNothing()
                .when(mockEventProbe)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        var result = evidenceFactory.create(kbvItem);

        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
        assertEquals(
                VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                getEvidenceAsMap(result).get("verificationScore"));
        assertEquals(
                ContraIndicator.V03.toString(), ((List) getEvidenceAsMap(result).get("ci")).get(0));
    }

    @Test
    void shouldFailWhenKbvItemStatusIsAnyOtherValue() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("some unknown value");

        doNothing()
                .when(mockEventProbe)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        var result = evidenceFactory.create(kbvItem);
        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        assertEquals(
                VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                getEvidenceAsMap(result).get("verificationScore"));
        assertNull(getEvidenceAsMap(result).get("ci"));
    }

    @Test
    void evidenceShouldContainCheckDetailsWhenKbvPasses() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));

        var result = evidenceFactory.create(kbvItem);

        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
        assertNotNull(getEvidenceAsMap(result).get("checkDetails"));
    }

    @Test
    void evidenceShouldHaveAsManyCheckDetailsItemsAsThereAreCorrectQuestionsWhenKbvPasses() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));

        var result = evidenceFactory.create(kbvItem);

        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
        var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");
        assertNotNull(checkDetailsResults);
        var results =
                objectMapper.convertValue(
                        checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
        assertEquals(3, results.size());
        results.forEach(
                checkDetail -> {
                    assertAll(
                            () -> {
                                assertEquals("kbv", checkDetail.getCheckMethod());
                                assertEquals(
                                        KbvQuality.LOW.getValue(), checkDetail.getKbvQuality());
                                assertEquals("multiple_choice", checkDetail.getKbvResponseMode());
                            });
                });
    }

    @Test
    void evidenceShouldHaveAsManyFailedCheckDetailsItemsAsThereAreInCorrectQuestionsWhenKbvFails() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("not Authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));

        doNothing()
                .when(mockEventProbe)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        var result = evidenceFactory.create(kbvItem);

        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
        var failedCheckDetailsResults = getEvidenceAsMap(result).get("failedCheckDetails");
        assertNotNull(failedCheckDetailsResults);
        var results =
                objectMapper.convertValue(
                        failedCheckDetailsResults, new TypeReference<List<CheckDetail>>() {});
        assertEquals(2, results.size());
        results.forEach(
                failedCheckDetail -> {
                    assertAll(
                            () -> {
                                assertEquals("kbv", failedCheckDetail.getCheckMethod());
                                assertNull(failedCheckDetail.getKbvQuality());
                                assertEquals(
                                        "multiple_choice", failedCheckDetail.getKbvResponseMode());
                            });
                });
    }

    @Test
    void evidenceShouldNotHaveAnyOfTheCheckDetailsWhenKbvStatusIsAnyOtherValue() {
        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus("some unknown value");

        doNothing()
                .when(mockEventProbe)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        var result = evidenceFactory.create(kbvItem);
        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

        assertEquals(
                VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                getEvidenceAsMap(result).get("verificationScore"));

        var failedCheckDetailsResults = getEvidenceAsMap(result).get("failedCheckDetails");
        var checkDetailsResults = getEvidenceAsMap(result).get("CheckDetails");

        assertNull(checkDetailsResults);
        assertNull(failedCheckDetailsResults);
    }

    private Map getEvidenceAsMap(Object[] result) {
        return (Map) result[0];
    }
}
