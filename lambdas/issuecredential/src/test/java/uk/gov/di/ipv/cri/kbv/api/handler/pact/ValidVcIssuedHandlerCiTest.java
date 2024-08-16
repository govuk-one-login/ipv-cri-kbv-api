package uk.gov.di.ipv.cri.kbv.api.handler.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.handler.IssueCredentialHandler;
import uk.gov.di.ipv.cri.kbv.api.handler.pact.states.DummyStates;
import uk.gov.di.ipv.cri.kbv.api.handler.pact.states.EvidenceCIState;
import uk.gov.di.ipv.cri.kbv.api.handler.pact.states.PersonIdentityDetailState;
import uk.gov.di.ipv.cri.kbv.api.handler.util.Injector;
import uk.gov.di.ipv.cri.kbv.api.handler.util.MockHttpServer;
import uk.gov.di.ipv.cri.kbv.api.service.EvidenceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.METRIC_DIMENSION_KBV_VERIFICATION;
import static uk.gov.di.ipv.cri.kbv.api.handler.util.JwtSigner.getEcdsaSigner;
import static uk.gov.di.ipv.cri.kbv.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;

@Tag("Pact")
@Provider("ExperianKbvCriVcProvider")
@PactFolder("pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
public class ValidVcIssuedHandlerCiTest
        implements DummyStates, PersonIdentityDetailState, EvidenceCIState, TestFixtures {
    @SystemStub EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Mock private KBVStorageService kbvStorageServiceMock;
    @Mock private SessionService sessionServiceMock;
    @Mock private EventProbe eventProbeMock;
    @Mock private AuditService auditServiceMock;
    @Mock private PersonIdentityService personIdentityServiceMock;
    @Mock private ConfigurationService configurationServiceMock;
    private VerifiableCredentialService verifiableCredentialService;
    private static final int PORT = 5020;
    private static final boolean ENABLE_FULL_DEBUG = true;
    public static final String SUBJECT = "test-subject";
    private final UUID sessionId = UUID.randomUUID();
    IssueCredentialHandler handler;
    private ObjectMapper objectMapper = getMapperWithCustomSerializers();
    private QuestionState questionState = new QuestionState();

    @BeforeAll
    static void setupServer() {
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.content_type.override.application/jwt", "text");
        System.setProperty("pact_do_not_track", "true");

        System.setProperty("pact.filter.description", "Valid credential request for VC with CI");
        if (ENABLE_FULL_DEBUG) {
            // AutoConfig SL4j with Log4J
            BasicConfigurator.configure();
            Configurator.setAllLevels("", Level.DEBUG);
        }
    }

    @AfterEach
    public void tearDown() {
        MockHttpServer.stopServer();
    }

    @BeforeEach
    void pactSetup(PactVerificationContext context)
            throws IOException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        environmentVariables.set("LAMBDA_TASK_ROOT", "handler");
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, "override");

        setupEventProbeBehaviour();

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(getEcdsaSigner());

        Clock clock = Clock.fixed(Instant.parse("2099-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
        VerifiableCredentialClaimsSetBuilder claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(configurationServiceMock, clock);
        claimsSetBuilder.overrideJti("dummyJti");

        Map<String, Integer> kbvQuestionQualityMapping =
                Map.of(
                        "1st Question", 3,
                        "2nd Question", 1,
                        "3rd Question", 2,
                        "4th Question", 0);

        EvidenceFactory evidenceFactory =
                new EvidenceFactory(objectMapper, eventProbeMock, kbvQuestionQualityMapping);

        verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory,
                        configurationServiceMock,
                        objectMapper,
                        claimsSetBuilder,
                        evidenceFactory);
        handler =
                new IssueCredentialHandler(
                        verifiableCredentialService,
                        kbvStorageServiceMock,
                        sessionServiceMock,
                        eventProbeMock,
                        auditServiceMock,
                        personIdentityServiceMock);

        MockHttpServer.startServer(
                new ArrayList<>(List.of(new Injector(handler, "/credential/issue", "/"))), PORT);

        context.setTarget(new HttpTestTarget("localhost", PORT));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTest(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("dummyExperianKbvComponentId is a valid issuer")
    @Override
    public void validDummyExperianKbvComponent() {
        when(configurationServiceMock.getMaxJwtTtl()).thenReturn(10L);
        when(configurationServiceMock.getParameterValue("JwtTtlUnit")).thenReturn("MINUTES");
        when(configurationServiceMock.getVerifiableCredentialIssuer())
                .thenReturn("dummyExperianKbvComponentId");
        when(configurationServiceMock.getVerifiableCredentialKmsSigningKeyId())
                .thenReturn("dummyKmsKeyId");
    }

    @State("dummyAccessToken is a valid access token")
    public void validDummyAccessToken() throws ParseException, JsonProcessingException {
        KBVItem kbvItem = kbvItemQuestionAskedStatesResultsSummary();

        when(sessionServiceMock.getSessionByAccessToken(getAccessToken()))
                .thenReturn(getSessionItem());
        when(kbvStorageServiceMock.getKBVItem(sessionId)).thenReturn(kbvItem);
        when(personIdentityServiceMock.getPersonIdentityDetailed(sessionId))
                .thenReturn(createPersonIdentity());
    }

    @NotNull
    private KBVItem kbvItemQuestionAskedStatesResultsSummary() throws JsonProcessingException {
        KBVItem kbvItem = kbvItemState3outOf4KbvWrong();
        kbvItem.setQuestionState(objectMapper.writeValueAsString(kbvCompletedIn3Batches()));
        return kbvItem;
    }

    @NotNull
    private KBVItem kbvItemState3outOf4KbvWrong() {
        KBVItem kbvItem = getKbvItem(sessionId, "dummyTxn");
        kbvItem.setStatus("not authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(4, 1, 3));
        return kbvItem;
    }

    private QuestionState kbvCompletedIn3Batches() {
        stateAfterAnsweringBatchWith("1st Question", "2nd Question");
        stateAfterAnsweringBatchWith("3rd Question");
        stateAfterAnsweringBatchWith("4th Question");

        return questionState;
    }

    private void stateAfterAnsweringBatchWith(String... questions) {
        List<KbvQuestion> kbvQuestions = getKbvQuestions(questions);
        List<QuestionAnswer> questionAnswers = getQuestionAnswers(questions);
        questionState.setQAPairs(kbvQuestions.toArray(KbvQuestion[]::new));
        this.questionState =
                loadKbvQuestionStateWithAnswers(questionState, kbvQuestions, questionAnswers);
    }

    @NotNull
    private AccessToken getAccessToken() throws ParseException {
        return AccessToken.parse("Bearer dummyAccessToken", AccessTokenType.BEARER);
    }

    @NotNull
    private SessionItem getSessionItem() {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);
        sessionItem.setAccessToken("Bearer dummyAccessToken");
        return sessionItem;
    }

    private void setupEventProbeBehaviour() {
        when(eventProbeMock.counterMetric(anyString())).thenReturn(eventProbeMock);
        doNothing()
                .when(eventProbeMock)
                .addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "fail"));
    }
}
