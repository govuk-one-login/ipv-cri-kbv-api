package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedBuilder;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvQuestion;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.service.EvidenceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.METRIC_DIMENSION_KBV_VERIFICATION;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;
import static uk.gov.di.ipv.cri.kbv.api.handler.IssueCredentialHandler.KBV_CREDENTIAL_ISSUER;
import static uk.gov.di.ipv.cri.kbv.api.handler.IssueCredentialHandler.NO_SUCH_ALGORITHM_ERROR;

@SuppressWarnings("rawtypes")
@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class IssueCredentialHandlerTest implements TestFixtures {

    @SystemStub
    @SuppressWarnings("unused")
    private final EnvironmentVariables environment =
            new EnvironmentVariables(
                    "JWT_TTL_UNIT",
                    "MINUTES",
                    ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID,
                    "override");

    private static final String SUBJECT = "subject";
    private static final UUID SESSION_ID = UUID.randomUUID();
    @Mock private Context context;
    @Mock private VerifiableCredentialService mockVerifiableCredentialService;
    @Mock private SessionService mockSessionService;

    @Mock private KBVStorageService mockKBVStorageService;

    @Mock private PersonIdentityService mockPersonIdentityService;
    @Mock private EventProbe mockEventProbe;
    @Mock private AuditService mockAuditService;
    @Captor private ArgumentCaptor<Map<String, Object>> auditEventExtensionsArgCaptor;
    @InjectMocks private IssueCredentialHandler handler;

    @Test
    void shouldReturn200OkWhenIssueCredentialRequestIsValid()
            throws JOSEException, SqsException, JsonProcessingException, NoSuchAlgorithmException {
        ArgumentCaptor<AuditEventContext> auditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        Map<String, String> requestHeaders =
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader());
        event.withHeaders(requestHeaders);
        setRequestBodyAsPlainJWT(event);

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(SESSION_ID);

        KBVItem kbvItem = new KBVItem();
        kbvItem.setStatus(VC_THIRD_PARTY_KBV_CHECK_PASS);
        kbvItem.setSessionId(SESSION_ID);
        kbvItem.setExpiryDate(Instant.now().plusSeconds(6000L).getEpochSecond());

        PersonIdentityDetailed personIdentity = createPersonIdentity();
        Map<String, Object> auditEventExtensions = Map.of("test", "audit-data");

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        when(mockPersonIdentityService.getPersonIdentityDetailed(SESSION_ID))
                .thenReturn(personIdentity);
        when(mockVerifiableCredentialService.getAuditEventExtensions(
                        kbvItem, sessionItem.getEvidenceRequest()))
                .thenReturn(auditEventExtensions);
        when(mockVerifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        sessionItem, personIdentity, kbvItem))
                .thenReturn(mock(SignedJWT.class));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(sessionItem, personIdentity, kbvItem);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER);
        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.VC_ISSUED),
                        auditEventContextArgCaptor.capture(),
                        auditEventExtensionsArgCaptor.capture());
        assertEquals(sessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
        assertEquals(requestHeaders, auditEventContextArgCaptor.getValue().getRequestHeaders());
        assertEquals(auditEventExtensions, auditEventExtensionsArgCaptor.getValue());
        verify(mockAuditService)
                .sendAuditEvent(eq(AuditEventType.END), auditEventContextArgCaptor.capture());
        assertEquals(sessionItem, auditEventContextArgCaptor.getValue().getSessionItem());
        assertEquals(
                ContentType.APPLICATION_JWT.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatusCode.OK, response.getStatusCode());
    }

    @Test
    void shouldThrowJOSEExceptionWhenGenerateVerifiableCredentialIsMalformed()
            throws JsonProcessingException, JOSEException, SqsException, NoSuchAlgorithmException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();
        var unExpectedJOSEException = new JOSEException("Unexpected JOSE object type: JWSObject");

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(SESSION_ID);

        KBVItem kbvItem = new KBVItem();
        kbvItem.setStatus(VC_THIRD_PARTY_KBV_CHECK_PASS);
        kbvItem.setSessionId(SESSION_ID);

        PersonIdentityDetailed personIdentity = createPersonIdentity();

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID)).thenReturn(kbvItem);
        when(mockPersonIdentityService.getPersonIdentityDetailed(SESSION_ID))
                .thenReturn(personIdentity);
        when(mockVerifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        sessionItem, personIdentity, kbvItem))
                .thenThrow(unExpectedJOSEException);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(sessionItem, personIdentity, kbvItem);
        verify(mockEventProbe).log(Level.ERROR, unExpectedJOSEException);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verifyNoMoreInteractions(mockVerifiableCredentialService);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        Map responseBody = new ObjectMapper().readValue(response.getBody(), Map.class);
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR.getCode(), responseBody.get("code"));
        assertEquals(
                ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR.getMessage(),
                responseBody.get("message"));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied()
            throws SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        setupEventProbeErrorBehaviour();

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursRetrievingASessionItemWithAccessToken()
            throws JsonProcessingException, SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                getHttpInternalServerError("AWS DynamoDbException Occurred");

        when(mockSessionService.getSessionByAccessToken(accessToken))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        String responseBody = new ObjectMapper().readValue(response.getBody(), String.class);
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertEquals(awsErrorDetails.errorMessage(), responseBody);
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void shouldThrowAccessDeniedErrorWhenRetrievingASessionItemNotFoundWithAnAccessToken(
            Class<? extends Throwable> exceptionClass)
            throws JsonProcessingException, SqsException {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenThrow(exceptionClass);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        Map<String, Object> responseBody =
                new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verify(mockEventProbe).log(any(), exceptionCaptor.capture());
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        verifyNoMoreInteractions(mockEventProbe);

        assertEquals(HttpStatusCode.FORBIDDEN, response.getStatusCode());
        assertThat(
                responseBody.get("error_description").toString(),
                containsString("Access denied by resource owner or authorization server"));
        assertEquals(exceptionClass, exceptionCaptor.getValue().getClass());
    }

    @Test
    void shouldThrowNoSuchAlgorithmErrorWhenTheWrongKeyAlgorithmIsUsed()
            throws NoSuchAlgorithmException,
                    JOSEException,
                    JsonProcessingException,
                    ParseException,
                    SqsException {
        String issuer = "issuer";
        String kmsSigningKeyId = "kmsSigningId";
        ObjectMapper objectMapper = new ObjectMapper();

        NoSuchAlgorithmException noSuchAlgorithmException =
                new NoSuchAlgorithmException(NO_SUCH_ALGORITHM_ERROR);
        UUID sessionId = UUID.randomUUID();

        AccessToken accessToken =
                AccessToken.parse("Bearer dummyAccessToken", AccessTokenType.BEARER);
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);
        sessionItem.setAccessToken("Bearer dummyAccessToken");

        KBVItem kbvItem = getKbvItem(sessionId, "dummyTxn");
        kbvItem.setStatus("authenticated");
        kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(3, 3, 0));

        QuestionState questionState = new QuestionState();
        stateAfterAnsweringBatchWith(questionState, "1st Question", "2nd Question");
        stateAfterAnsweringBatchWith(questionState, "3rd Question");
        kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));

        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(10L);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(issuer);
        when(mockConfigurationService.getVerifiableCredentialKmsSigningKeyId())
                .thenReturn(kmsSigningKeyId);

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockKBVStorageService.getKBVItem(sessionId)).thenReturn(kbvItem);
        when(mockPersonIdentityService.getPersonIdentityDetailed(sessionId))
                .thenReturn(createPersonIdentity());
        SignedJWTFactory mockedSignedClaimSetJwt = mock(SignedJWTFactory.class);

        when(mockedSignedClaimSetJwt.createSignedJwt(
                        any(JWTClaimsSet.class), eq(issuer), eq(kmsSigningKeyId)))
                .thenThrow(noSuchAlgorithmException);

        Clock clock = Clock.fixed(Instant.parse("2099-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
        VerifiableCredentialClaimsSetBuilder claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);
        claimsSetBuilder.overrideJti("dummyJti");

        Map<String, Integer> kbvQuestionQualityMapping =
                Map.of(
                        "1st Question", 2,
                        "2nd Question", 2,
                        "3rd Question", 1);

        EvidenceFactory evidenceFactory =
                new EvidenceFactory(objectMapper, mockEventProbe, kbvQuestionQualityMapping);

        VerifiableCredentialService verifiableCredentialService =
                new VerifiableCredentialService(
                        mockedSignedClaimSetJwt,
                        mockConfigurationService,
                        objectMapper,
                        claimsSetBuilder,
                        evidenceFactory);
        handler =
                new IssueCredentialHandler(
                        verifiableCredentialService,
                        mockKBVStorageService,
                        mockSessionService,
                        mockEventProbe,
                        mockAuditService,
                        mockPersonIdentityService);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verify(mockEventProbe).log(any(), exceptionCaptor.capture());
        verify(mockEventProbe).addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, "pass"));
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        verifyNoMoreInteractions(mockEventProbe);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.getBody(), containsString(NO_SUCH_ALGORITHM_ERROR));
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursDuringRetrievingAnAddressItemWithSessionId()
            throws JsonProcessingException, SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                getHttpInternalServerError("AWS DynamoDbException Occurred");

        SessionItem mockSessionItem = mock(SessionItem.class);
        when(mockSessionItem.getSessionId()).thenReturn(SESSION_ID);
        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(mockSessionItem);
        when(mockKBVStorageService.getKBVItem(SESSION_ID))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockKBVStorageService).getKBVItem(SESSION_ID);
        verify(mockEventProbe).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        String responseBody = new ObjectMapper().readValue(response.getBody(), String.class);
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertEquals(awsErrorDetails.errorMessage(), responseBody);
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(KBV_CREDENTIAL_ISSUER, 0d)).thenReturn(mockEventProbe);
    }

    private void setRequestBodyAsPlainJWT(APIGatewayProxyRequestEvent event) {
        String requestJWT =
                new PlainJWT(
                                new JWTClaimsSet.Builder()
                                        .claim(JWTClaimNames.SUBJECT, SUBJECT)
                                        .build())
                        .serialize();

        event.setBody(requestJWT);
    }

    private PersonIdentityDetailed createPersonIdentity() {
        Address address = new Address();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");

        Name name = new Name();
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Bloggs");

        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Bloggs");
        name.setNameParts(List.of(firstNamePart, surnamePart));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1980, 1, 1));

        return PersonIdentityDetailedBuilder.builder(List.of(name), List.of(birthDate))
                .withAddresses(List.of(address))
                .build();
    }

    private void stateAfterAnsweringBatchWith(QuestionState questionState, String... questions) {
        List<KbvQuestion> kbvQuestions = getKbvQuestions(questions);
        List<QuestionAnswer> questionAnswers = getQuestionAnswers(questions);
        questionState.setQAPairs(kbvQuestions.toArray(KbvQuestion[]::new));

        loadKbvQuestionStateWithAnswers(questionState, kbvQuestions, questionAnswers);
    }

    private AwsErrorDetails getHttpInternalServerError(String message) {
        return AwsErrorDetails.builder()
                .errorCode("")
                .sdkHttpResponse(
                        SdkHttpResponse.builder()
                                .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                                .build())
                .errorMessage(message)
                .build();
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of(
                        SessionNotFoundException.class, "no session found with that access token"),
                Arguments.of(SessionExpiredException.class, "session expired"),
                Arguments.of(AccessTokenExpiredException.class, "access code expired"));
    }
}
