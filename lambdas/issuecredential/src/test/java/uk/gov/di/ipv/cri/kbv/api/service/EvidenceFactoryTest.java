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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE;

@ExtendWith(MockitoExtension.class)
class EvidenceFactoryTest implements TestFixtures {
    private static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";
    private EvidenceFactory evidenceFactory;
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());
    @Mock private EventProbe mockEventProbe;
    @Mock private EvidenceRequest mockEvidenceRequest;

    @BeforeEach
    void setUp() {
        evidenceFactory =
                new EvidenceFactory(
                        objectMapper, mockEventProbe, KBV_QUESTION_QUALITY_MAPPING_SERIALIZED);
    }

    @Nested
    class EvidenceVerificationScore {
        @Nested
        class ThreeOutOfFourPrioritisedKbvQuestionStrategy {
            @Test
            @DisplayName(
                    "KBV passes using the default pass verification of 2 if verification score from session item is a Zero")
            void hasAVerificationScoreOfTwoIfVerificationScoreIsZero()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(0);
                sessionItem.setEvidenceRequest(evidenceRequest);

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, evidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VC_PASS_EVIDENCE_SCORE, getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }

            @ParameterizedTest
            @DisplayName(
                    "KBV passes using the actual verification score from session item if present")
            @CsvSource({"1", "2"})
            void hasAVerificationScoreOf(int score) throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(score);
                sessionItem.setEvidenceRequest(evidenceRequest);

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        evidenceRequest.getVerificationScore(),
                        getEvidenceAsMap(result).get("verificationScore"));
            }

            @Test
            @DisplayName(
                    "KBV passes using the default verification score of 2 if score in session item is absent")
            void hasAVerificationScoreOfTwoWhenMissingScore() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");
                SessionItem sessionItem = new SessionItem();

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VC_PASS_EVIDENCE_SCORE, getEvidenceAsMap(result).get("verificationScore"));
            }

            @ParameterizedTest
            @CsvSource({"3", "4", "5"})
            @DisplayName("throws IllegalStateException if verification score is not supported")
            void throwsIllegalStateExceptionWhenGivenValueGreaterThanTwo(int verificationScore)
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(verificationScore);
                sessionItem.setEvidenceRequest(evidenceRequest);

                Executable executable =
                        () -> evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                IllegalStateException exception =
                        assertThrows(IllegalStateException.class, executable);

                verifyNoMoreInteractions(mockEventProbe);
                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        String.format("Verification Score %d is not supported", verificationScore),
                        exception.getMessage());
            }

            @Test
            @DisplayName("KBV fails with an unknown status no contraIndicator is returned")
            void failsWhenKbvItemStatusIsAnyOtherValue() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("some unknown value");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);
                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }
        }

        @Nested
        class TwoOutOfThreePrioritisedKbvQuestionStrategy {
            @BeforeEach
            void setUp() {
                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                KBV_QUESTION_QUALITY_MAPPING_SERIALIZED);
                evidenceFactory.setKbvQuestionStrategy("2 out of 3 Prioritised");
            }

            @Test
            @DisplayName(
                    "KBV passes using the default pass verification of 2 if verification score from session item is Zero")
            void hasAVerificationScoreOfTwoIfVerificationScoreIsZero()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(0);
                sessionItem.setEvidenceRequest(evidenceRequest);

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var result = evidenceFactory.create(kbvItem, evidenceRequest);
                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VC_PASS_EVIDENCE_SCORE, getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }

            @ParameterizedTest
            @DisplayName(
                    "KBV passes using the actual verification score from session item if present")
            @CsvSource({"1", "2"})
            void hasAVerificationScoreOf(int score) throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(score);
                sessionItem.setEvidenceRequest(evidenceRequest);

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        evidenceRequest.getVerificationScore(),
                        getEvidenceAsMap(result).get("verificationScore"));
            }

            @Test
            @DisplayName(
                    "KBV passes using the default verification score of 2 if score in session item is absent")
            void hasAVerificationScoreOfTwoWhenMissingScore() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                SessionItem sessionItem = new SessionItem();

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VC_PASS_EVIDENCE_SCORE, getEvidenceAsMap(result).get("verificationScore"));
            }

            @ParameterizedTest
            @CsvSource({"3", "4", "5"})
            @DisplayName("throws IllegalStateException if verification score is not supported")
            void throwsIllegalStateExceptionWhenGivenValueGreaterThanTwo(int verificationScore)
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                UUID sessionId = UUID.randomUUID();
                kbvItem.setSessionId(sessionId);
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                SessionItem sessionItem = new SessionItem();
                EvidenceRequest evidenceRequest = new EvidenceRequest();
                evidenceRequest.setVerificationScore(verificationScore);
                sessionItem.setEvidenceRequest(evidenceRequest);

                Executable executable =
                        () -> evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest());

                IllegalStateException exception =
                        assertThrows(IllegalStateException.class, executable);

                verifyNoMoreInteractions(mockEventProbe);
                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        String.format("Verification Score %d is not supported", verificationScore),
                        exception.getMessage());
            }

            @Test
            @DisplayName("KBV fails with an unknown status no contraIndicator is returned")
            void failsWhenKbvItemStatusIsAnyOtherValue() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("some unknown value");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);
                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }
        }
    }

    @Nested
    class KbvAuthenticationStatus {
        @Nested
        class ThreeOutOfFourPrioritisedKbvQuestionStrategy {
            @Test
            @DisplayName(
                    "KBV passes with 'Authenticated' status when 3 answers are correct out of 3 that are asked")
            void passesWhenKbvItemStatusIsAuthenticated() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }

            @Test
            @DisplayName(
                    "KBV fails with 'not authenticated' status and has a contraIndicator when more than 1 answers are incorrect out of 4 asked")
            void failsThenReturnsV03ContraIndicatorWhenMultipleAnswersAreIncorrect()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("not authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertEquals(
                        ContraIndicator.V03.toString(),
                        ((List) getEvidenceAsMap(result).get("ci")).get(0));
            }

            @Test
            @DisplayName(
                    "KBV fails and returns with 'unable to authenticate' status and a contraIndicator when a user with exactly 3 KBV(s) with the 3rd-party received 2 questions and got one wrong and no further question can be received from 3rd party")
            void
                    failsThenReturnsAV03ContraIndicatorWhenAUserWithExactly3KbvWithThe3rdPartyGetsOneOfTwoQuestionsWrong()
                            throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("unable to authenticate");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 1, 1));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertEquals(
                        ContraIndicator.V03.toString(),
                        ((List) getEvidenceAsMap(result).get("ci")).get(0));
            }

            @Test
            @DisplayName("KBV fails with an unknown status no contraIndicator is returned")
            void failsWhenKbvItemStatusIsAnyOtherValue() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("some unknown value");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);
                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }
        }

        @Nested
        class TwoOutOfThreePrioritisedKbvQuestionStrategy {
            @BeforeEach
            void setUp() {
                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                KBV_QUESTION_QUALITY_MAPPING_SERIALIZED);
                evidenceFactory.setKbvQuestionStrategy("2 out of 3 Prioritised");
            }

            @Test
            @DisplayName(
                    "KBV passes with 'Authenticated' status when 2 answers are correct out of 2 that are asked")
            void passesWhenKbvItemStatusIsAuthenticated() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }

            @Test
            @DisplayName(
                    "KBV fails with 'not authenticated' status and has a contraIndicator when more than 1 answers are incorrect out of 3 asked")
            void failsThenReturnsV03ContraIndicatorWhenMultipleAnswersAreIncorrect()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("not authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 1, 2));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertEquals(
                        ContraIndicator.V03.toString(),
                        ((List) getEvidenceAsMap(result).get("ci")).get(0));
            }

            @Test
            @DisplayName(
                    "KBV fails and returns with 'unable to authenticate' status and a contraIndicator when a user with exactly 3 KBV with the 3rd-party received 2 questions and got one wrong and no further question can be received from 3rd party")
            void
                    failsThenReturnsAV03ContraIndicatorWhenAUserWithExactly3KbvWithThe3rdPartyGetsOneOfTwoQuestionsWrong()
                            throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("unable to authenticate");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 1, 1));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertEquals(
                        ContraIndicator.V03.toString(),
                        ((List) getEvidenceAsMap(result).get("ci")).get(0));
            }

            @Test
            @DisplayName("KBV fails with an unknown status no contraIndicator is returned")
            void failsWhenKbvItemStatusIsAnyOtherValue() throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("some unknown value");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);
                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                assertEquals(
                        VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE,
                        getEvidenceAsMap(result).get("verificationScore"));
                assertNull(getEvidenceAsMap(result).get("ci"));
            }
        }
    }

    @Nested
    class EvidenceCheckDetails {
        @Nested
        class ThreeOutOfFourPrioritisedKbvQuestionStrategy {
            @Test
            @DisplayName(
                    "KBV passes and three check details are produced for all 3 answered out of 3 asked")
            void shouldHaveThreeCheckDetailsItemsForThreeCorrectlyAnsweredQuestionsWhenKbvPasses()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
                List<QuestionAnswer> questionFirstSecondAnswers =
                        getQuestionAnswers("First", "Second");
                List<KbvQuestion> kbvQuestionsThird = getKbvQuestions("Third");
                List<QuestionAnswer> questionThirdAnswers = getQuestionAnswers("Third");
                QuestionState questionState = new QuestionState();
                questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));
                questionState =
                        loadKbvQuestionStateWithAnswers(
                                questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

                questionState.setQAPairs(kbvQuestionsThird.toArray(KbvQuestion[]::new));
                questionState =
                        loadKbvQuestionStateWithAnswers(
                                questionState, kbvQuestionsThird, questionThirdAnswers);

                kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));
                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                objectMapper.readValue(
                                        "{\"First\":9,\"Second\": 5, \"Third\": 4}", Map.class));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

                var results =
                        objectMapper.convertValue(
                                checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
                assertEquals(3, results.size());
                assertNotNull(checkDetailsResults);
                assertNull(getEvidenceAsMap(result).get("failedCheckDetails"));
                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());

                assertAll(
                        () -> {
                            assertEquals("kbv", results.get(0).getCheckMethod());
                            assertEquals(9, results.get(0).getKbvQuality());
                            assertEquals("multiple_choice", results.get(0).getKbvResponseMode());
                            assertEquals("kbv", results.get(1).getCheckMethod());
                            assertEquals(5, results.get(1).getKbvQuality());
                            assertEquals("multiple_choice", results.get(1).getKbvResponseMode());
                            assertEquals("kbv", results.get(2).getCheckMethod());
                            assertEquals(4, results.get(2).getKbvQuality());
                            assertEquals("multiple_choice", results.get(2).getKbvResponseMode());
                        });
                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(3, results.size());
                assertEquals(9, results.get(0).getKbvQuality());
                assertEquals(5, results.get(1).getKbvQuality());
                assertEquals(4, results.get(2).getKbvQuality());
            }

            @Test
            @DisplayName(
                    "should create check details with the lowest 3 kbvQuality values, so we are excluding the highest kbvQuality value out of 4 possible question asked for which it's not known which is incorrect")
            void shouldReturnLowerKbvQualityWhenOnlyOneQuestionWasInitiallyAnsweredCorrectly()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 3, 1));
                List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
                List<QuestionAnswer> questionFirstSecondAnswers =
                        getQuestionAnswers("First", "Second");
                List<KbvQuestion> kbvQuestionsThirdFourth = getKbvQuestions("Third", "Fourth");
                List<QuestionAnswer> questionThirdFourthAnswers =
                        getQuestionAnswers("Third", "Fourth");
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
                                        "{\"First\":9,\"Second\": 5, \"Third\": 4, \"Fourth\": 8}",
                                        Map.class));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

                var results =
                        objectMapper.convertValue(
                                checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
                assertNotNull(checkDetailsResults);
                assertNotNull(getEvidenceAsMap(result).get("failedCheckDetails"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(3, results.size());
                assertEquals(4, results.get(0).getKbvQuality());
                assertEquals(5, results.get(1).getKbvQuality());
                assertEquals(8, results.get(2).getKbvQuality());
            }

            @Test
            @DisplayName(
                    "should create check details with 3 kbv quality mapped exactly to there questions since the 1st question of the next batch is wrong, it was excluded and all inclusions were mapped correctly")
            void shouldReturnActualKbvQualityWhenInitialTwoQuestionsAreAnsweredCorrectly()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 3, 1));
                List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
                List<QuestionAnswer> questionFirstSecondAnswers =
                        getQuestionAnswers("First", "Second");
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
                                        "{\"First\":9,\"Second\": 5, \"Third\": 4, \"Fourth\": 8}",
                                        Map.class));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

                var results =
                        objectMapper.convertValue(
                                checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
                assertNotNull(checkDetailsResults);
                assertNotNull(getEvidenceAsMap(result).get("failedCheckDetails"));

                assertEquals("3 out of 4 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(3, results.size());
                assertEquals(9, results.get(0).getKbvQuality());
                assertEquals(5, results.get(1).getKbvQuality());
                assertEquals(8, results.get(2).getKbvQuality());
            }

            @Test
            @DisplayName(
                    "should create two check details and two failed check details, check details would have been assigned to lower kbv Quality values")
            void shouldHaveTwoFailedAndTwoCheckDetailsOutOfFourKbvQuestionsAsked()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("not Authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 2, 2));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third", "Fourth");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
                var failedCheckDetailsResultsNode =
                        getEvidenceAsMap(result).get("failedCheckDetails");
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
        }

        @Nested
        class TwoOutOfThreePrioritisedKbvQuestionStrategy {
            @BeforeEach
            void setUp() {
                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                KBV_QUESTION_QUALITY_MAPPING_SERIALIZED);
                evidenceFactory.setKbvQuestionStrategy("2 out of 3 Prioritised");
            }

            @Test
            @DisplayName(
                    "KBV passes and two check details are produced for all 2 answered out of 2 asked")
            void shouldHaveTwoCheckDetailsItemsForTwoCorrectlyAnsweredQuestionsWhenKbvPasses()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 2, 0));
                setKbvItemQuestionState(kbvItem, "First", "Second");

                List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
                List<QuestionAnswer> questionFirstSecondAnswers =
                        getQuestionAnswers("First", "Second");

                QuestionState questionState = new QuestionState();
                questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));
                questionState =
                        loadKbvQuestionStateWithAnswers(
                                questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

                kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));
                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                objectMapper.readValue("{\"First\":9,\"Second\": 5}", Map.class));
                evidenceFactory.setKbvQuestionStrategy("2 out of 3 Prioritised");

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

                var results =
                        objectMapper.convertValue(
                                checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
                assertEquals(2, results.size());
                assertNotNull(checkDetailsResults);
                assertNull(getEvidenceAsMap(result).get("failedCheckDetails"));
                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertAll(
                        () -> {
                            assertEquals("kbv", results.get(0).getCheckMethod());
                            assertEquals(9, results.get(0).getKbvQuality());
                            assertEquals("multiple_choice", results.get(0).getKbvResponseMode());
                            assertEquals("kbv", results.get(1).getCheckMethod());
                            assertEquals(5, results.get(1).getKbvQuality());
                        });
                assertEquals(2, results.size());
                assertEquals(9, results.get(0).getKbvQuality());
                assertEquals(5, results.get(1).getKbvQuality());
            }

            @Test
            @DisplayName(
                    "should create check details with the lowest 2 kbvQuality values, so we are excluding the highest kbvQuality value out of 3 possible questions asked since we don't know which of the initial 2 is incorrect")
            void shouldReturnActualKbvQualityWhenInitialTwoQuestionsAreAnsweredCorrectly()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 2, 1));
                List<KbvQuestion> kbvQuestionsFirstSecond = getKbvQuestions("First", "Second");
                List<QuestionAnswer> questionFirstSecondAnswers =
                        getQuestionAnswers("First", "Second");
                List<KbvQuestion> kbvQuestionsThird = getKbvQuestions("Third");
                List<QuestionAnswer> questionThirdAnswers = getQuestionAnswers("Third");

                QuestionState questionState = new QuestionState();
                questionState.setQAPairs(kbvQuestionsFirstSecond.toArray(KbvQuestion[]::new));
                questionState =
                        loadKbvQuestionStateWithAnswers(
                                questionState, kbvQuestionsFirstSecond, questionFirstSecondAnswers);

                questionState.setQAPairs(kbvQuestionsThird.toArray(KbvQuestion[]::new));
                questionState =
                        loadKbvQuestionStateWithAnswers(
                                questionState, kbvQuestionsThird, questionThirdAnswers);

                kbvItem.setQuestionState(new ObjectMapper().writeValueAsString(questionState));

                evidenceFactory =
                        new EvidenceFactory(
                                objectMapper,
                                mockEventProbe,
                                objectMapper.readValue(
                                        "{\"First\":9,\"Second\": 5, \"Third\": 4}", Map.class));
                evidenceFactory.setKbvQuestionStrategy("2 out of 3 Prioritised");

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
                var checkDetailsResults = getEvidenceAsMap(result).get("checkDetails");

                var results =
                        objectMapper.convertValue(
                                checkDetailsResults, new TypeReference<List<CheckDetail>>() {});
                assertNotNull(checkDetailsResults);
                assertNotNull(getEvidenceAsMap(result).get("failedCheckDetails"));

                assertEquals("2 out of 3 Prioritised", evidenceFactory.getKbvQuestionStrategy());
                assertEquals(2, results.size());
                assertEquals(4, results.get(0).getKbvQuality());
                assertEquals(5, results.get(1).getKbvQuality());
            }

            @Test
            @DisplayName(
                    "should create two check details and one failed check details, check details would have been assigned to lower kbv Quality values")
            void shouldHaveTwoFailedAndOneCheckDetailsOutOfThreeKbvQuestionsAsked()
                    throws JsonProcessingException {
                KBVItem kbvItem = getKbvItem();
                kbvItem.setStatus("not Authenticated");
                kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 1, 2));
                setKbvItemQuestionState(kbvItem, "First", "Second", "Third");

                doNothing()
                        .when(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));

                var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

                verify(mockEventProbe)
                        .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
                var failedCheckDetailsResultsNode =
                        getEvidenceAsMap(result).get("failedCheckDetails");
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
                assertEquals(1, checkDetailsResults.size());
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

            var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

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

            var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);

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

            var result = evidenceFactory.create(kbvItem, mockEvidenceRequest);
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
                            IllegalStateException.class,
                            () -> evidenceFactory.create(kbvItem, mockEvidenceRequest));

            assertEquals("QuestionId: NA may not be present in Mapping", thrown.getMessage());
        }
    }

    private Map getEvidenceAsMap(Object[] result) {
        return (Map) result[0];
    }
}
