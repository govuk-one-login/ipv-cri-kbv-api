package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuality;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceFactoryTest implements TestFixtures {
    private static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
    private EvidenceFactory evidenceFactory;
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());
    @Mock private EventProbe mockEventProbe;
    @Mock private SessionService mockSessionService;

    @BeforeEach
    void setUp() {
        evidenceFactory =
                new EvidenceFactory(
                        objectMapper,
                        mockEventProbe,
                        KBV_QUESTION_QUALITY_MAPPING_SERIALIZED,
                        mockSessionService);
    }

    @Nested
    class EvidenceVerificationScore {
        @Test
        void shouldPassWhenKbvItemStatusIsAuthenticated() throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

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
        void shouldFailAndReturnContraIndicatorWhenMultipleAnswersAreIncorrect()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("not Authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");

            doNothing()
                    .when(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            assertEquals(
                    VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                    getEvidenceAsMap(result).get("verificationScore"));
            assertEquals(
                    ContraIndicator.V03.toString(),
                    ((List) getEvidenceAsMap(result).get("ci")).get(0));
        }

        @Test
        void shouldFailAndReturnContraIndicatorWhenEnoughAnswersAreIncorrect()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("unable to authenticate");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 2, 1));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

            doNothing()
                    .when(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            assertEquals(
                    VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                    getEvidenceAsMap(result).get("verificationScore"));
            assertEquals(
                    ContraIndicator.V03.toString(),
                    ((List) getEvidenceAsMap(result).get("ci")).get(0));
        }

        @Test
        void shouldFailWhenKbvItemStatusIsAnyOtherValue() throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
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
    }

    @Nested
    class EvidenceCheckDetails {
        @Test
        @DisplayName("should have the verification score from session item if present")
        void shouldHaveAVerificationScoreOfOne() throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            UUID sessionId = UUID.randomUUID();
            kbvItem.setSessionId(sessionId);
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

            SessionItem mockSessionItem = new SessionItem();
            // mockSessionItem.setEvidence(...);

            when(mockSessionService.getSession(sessionId.toString())).thenReturn(mockSessionItem);

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            assertNotNull(
                    getEvidenceAsMap(result)
                            .get("checkDetails")); // TODO: assert that the verification_score ==
            // mockSessionItem verificationScore
        }

        @Test
        @DisplayName(
                "should have the verification score of 2 when no evidence block in SessionItem present")
        void shouldHaveAVerificationScoreOfTwo() throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            UUID sessionId = UUID.randomUUID();
            kbvItem.setSessionId(sessionId);
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

            when(mockSessionService.getSession(sessionId.toString())).thenReturn(new SessionItem());

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            assertNotNull(
                    getEvidenceAsMap(result)
                            .get("checkDetails")); // TODO: assert that the verification_score == 2
        }

        @Test
        void shouldContainCheckDetailsWhenKbvPasses() throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            assertNotNull(getEvidenceAsMap(result).get("checkDetails"));
        }

        @Test
        void shouldHaveAsManyCheckDetailsItemsAsThereAreCorrectQuestionsWhenKbvPasses()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third");
            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

            var results =
                    objectMapper.convertValue(
                            checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
            assertEquals(3, results.size());
            assertNotNull(checkDetailsResults);
            assertNull(getEvidenceAsMap(result).get("failedCheckDetails"));
            results.forEach(
                    checkDetail -> {
                        assertAll(
                                () -> {
                                    assertEquals("kbv", checkDetail.getCheckMethod());
                                    assertEquals(
                                            KbvQuality.LOW.getValue(), checkDetail.getKbvQuality());
                                    assertEquals(
                                            "multiple_choice", checkDetail.getKbvResponseMode());
                                });
                    });
        }

        @Test
        void shouldReturnLowerKbvQualityWhenOnlyOneQuestionWasInitiallyAnsweredCorrectly()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 3, 1));
            List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
            List<QuestionAnswer> questionFirstSecondAnswers = getQuestionAnswers("First", "Second");
            List<KbvQuestion> kbvQuestionsThirdFourth = getKbvQuestions("Third", "Fourth");
            List<QuestionAnswer> questionThirdFourthAnswers = getQuestionAnswers("Third", "Fourth");
            QuestionState questionState = new QuestionState();
            questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));
            questionState =
                    loadKbvQuestionStateWithAnswers(
                            questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

            questionState.setQAPairs(kbvQuestionsThirdFourth.toArray(KbvQuestion[]::new));
            questionState =
                    loadKbvQuestionStateWithAnswers(
                            questionState, kbvQuestionsThirdFourth, questionThirdFourthAnswers);

            kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));

            evidenceFactory =
                    new EvidenceFactory(
                            objectMapper,
                            mockEventProbe,
                            objectMapper.readValue(
                                    "{\"First\":4,\"Second\": 5, \"Third\": 9, \"Fourth\": 9}",
                                    Map.class),
                            mockSessionService);

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

            var results =
                    objectMapper.convertValue(
                            checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
            assertNotNull(checkDetailsResults);
            assertNotNull(getEvidenceAsMap(result).get("failedCheckDetails"));

            assertEquals(3, results.size());
            assertEquals(4, results.get(0).getKbvQuality());
            assertEquals(5, results.get(1).getKbvQuality());
            assertEquals(9, results.get(2).getKbvQuality());
        }

        @Test
        void shouldReturnActualKbvQualityWhenInitial2QuestionsAreAnsweredCorrectly()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 3, 1));
            List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
            List<QuestionAnswer> questionFirstSecondAnswers = getQuestionAnswers("First", "Second");
            List<KbvQuestion> kbvQuestionsThird = getKbvQuestions("Third");
            List<QuestionAnswer> questionThirdAnswers = getQuestionAnswers("Third");
            List<KbvQuestion> kbvQuestionsFourth = getKbvQuestions("Fourth");
            List<QuestionAnswer> questionFourthAnswers = getQuestionAnswers("Fourth");

            QuestionState questionState = new QuestionState();
            questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));
            questionState =
                    loadKbvQuestionStateWithAnswers(
                            questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

            questionState.setQAPairs(kbvQuestionsThird.toArray(KbvQuestion[]::new));
            questionState =
                    loadKbvQuestionStateWithAnswers(
                            questionState, kbvQuestionsThird, questionThirdAnswers);

            questionState.setQAPairs(kbvQuestionsFourth.toArray(KbvQuestion[]::new));
            questionState =
                    loadKbvQuestionStateWithAnswers(
                            questionState, kbvQuestionsFourth, questionFourthAnswers);

            kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));

            evidenceFactory =
                    new EvidenceFactory(
                            objectMapper,
                            mockEventProbe,
                            objectMapper.readValue(
                                    "{\"First\":4,\"Second\": 5, \"Third\": 9, \"Fourth\": 8}",
                                    Map.class),
                            mockSessionService);

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
            var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

            var results =
                    objectMapper.convertValue(
                            checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
            assertNotNull(checkDetailsResults);
            assertNotNull(getEvidenceAsMap(result).get("failedCheckDetails"));

            assertEquals(3, results.size());
            assertEquals(4, results.get(0).getKbvQuality());
            assertEquals(5, results.get(1).getKbvQuality());
            assertEquals(8, results.get(2).getKbvQuality());
        }

        @Test
        void shouldHave2FailedAnd2CheckDetails2of4KbvQuestionsAreInCorrect()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("not Authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");

            doNothing()
                    .when(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            var failedCheckDetailsResultsNode = getEvidenceAsMap(result).get("failedCheckDetails");
            var checkDetailsResultsNode = getEvidenceAsMap(result).get("checkDetails");
            var checkDetailsResults =
                    objectMapper.convertValue(
                            checkDetailsResultsNode, new TypeReference<List<CheckDetail>>() {});
            var failedCheckDetailsResults =
                    objectMapper.convertValue(
                            failedCheckDetailsResultsNode,
                            new TypeReference<List<CheckDetail>>() {});

            assertNotNull(failedCheckDetailsResultsNode);
            assertEquals(2, failedCheckDetailsResults.size());
            assertEquals(2, checkDetailsResults.size());
            failedCheckDetailsResults.forEach(
                    failedCheckDetail -> {
                        assertAll(
                                () -> {
                                    assertEquals("kbv", failedCheckDetail.getCheckMethod());
                                    assertNull(failedCheckDetail.getKbvQuality());
                                    assertEquals(
                                            "multiple_choice",
                                            failedCheckDetail.getKbvResponseMode());
                                });
                    });
        }

        @Test
        void showNoCheckDetailsWhenKbvStatusIsUnAuthenticatedButQuestionAnswerResultSummaryIsNull()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("not Authenticated");
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");
            kbvItem.setQuestionAnswerResultSummary(null);

            doNothing()
                    .when(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            var failedCheckDetailsResults = getEvidenceAsMap(result).get("failedCheckDetails");
            var checkDetailsResults = getEvidenceAsMap(result).get("CheckDetails");

            assertNull(checkDetailsResults);
            assertNull(failedCheckDetailsResults);
        }

        @Test
        void showNoCheckDetailsWhenKbvStatusIsUnAuthenticatedButQuestionAskedIsZero()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("not Authenticated");
            setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(0, 0, 0));

            doNothing()
                    .when(mockEventProbe)
                    .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

            var result = evidenceFactory.create(kbvItem);

            verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
            var failedCheckDetailsResults = getEvidenceAsMap(result).get("failedCheckDetails");
            var checkDetailsResults = getEvidenceAsMap(result).get("CheckDetails");

            assertNull(checkDetailsResults);
            assertNull(failedCheckDetailsResults);
        }

        @Test
        void shouldNotHaveAnyOfTheCheckDetailsWhenKbvStatusIsAnyOtherValue()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
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

        @Test
        void shouldFailWhenKbvItemEvidenceMappingDoesNotIncludeQuestion()
                throws JsonProcessingException {
            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
            setKbvItemQuestionState(kbvItem, "NA");

            IllegalStateException thrown =
                    assertThrows(
                            IllegalStateException.class, () -> evidenceFactory.create(kbvItem));

            assertEquals("QuestionId: NA may not be present in Mapping", thrown.getMessage());
        }
    }

    private Map getEvidenceAsMap(Object[] result) {
        return (Map) result[0];
    }
}
