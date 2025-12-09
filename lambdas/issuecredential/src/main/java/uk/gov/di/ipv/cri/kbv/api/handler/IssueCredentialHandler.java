package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.TempCleaner;
import uk.gov.di.ipv.cri.kbv.api.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.VerifiableCredentialService;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import static org.apache.logging.log4j.Level.ERROR;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.ACCESS_TOKEN_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String NO_SUCH_ALGORITHM_ERROR =
            "The algorithm name provided is incorrect or misspelled, should be ES256.";
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String KBV_CREDENTIAL_ISSUER = "kbv_credentials_issued";
    private static final String CREDENTIAL_NOT_ISSUED = "credential not issued";
    private static final String CREDENTIAL_ISSUED = "credential issued";
    private static final String VC_MESSAGE_FORMAT = "{}: {}";
    private final VerifiableCredentialService verifiableCredentialService;
    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;
    private final PersonIdentityService personIdentityService;

    public IssueCredentialHandler(
            VerifiableCredentialService verifiableCredentialService,
            KBVStorageService kbvStorageService,
            SessionService sessionService,
            EventProbe eventProbe,
            AuditService auditService,
            PersonIdentityService personIdentityService) {
        this.verifiableCredentialService = verifiableCredentialService;
        this.kbvStorageService = kbvStorageService;
        this.sessionService = sessionService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
        this.personIdentityService = personIdentityService;
    }

    @ExcludeFromGeneratedCoverageReport
    public IssueCredentialHandler() throws JsonProcessingException {
        TempCleaner.clean();
        ServiceFactory serviceFactory = new ServiceFactory();
        DynamoDbEnhancedClient dynamoDbEnhancedClient = serviceFactory.getDynamoDbEnhancedClient();
        ConfigurationService configurationService = serviceFactory.getConfigurationService();

        this.verifiableCredentialService =
                new VerifiableCredentialService(
                        configurationService, serviceFactory.getKMSClient());
        this.kbvStorageService = new KBVStorageService(dynamoDbEnhancedClient);
        this.sessionService = serviceFactory.getSessionService();
        this.eventProbe = new EventProbe();
        this.auditService = serviceFactory.getAuditService();
        this.personIdentityService =
                new PersonIdentityService(
                        configurationService, serviceFactory.getDynamoDbEnhancedClient());
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPaths.API_GATEWAY_REST, clearState = true)
    @FlushMetrics(namespace = "di-ipv-cri-kbv-api", captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            var accessToken = validateInputHeaderBearerToken(input.getHeaders());
            var sessionItem = this.sessionService.getSessionByAccessToken(accessToken);
            var kbvItem = kbvStorageService.getKBVItem(sessionItem.getSessionId());
            var personIdentity =
                    personIdentityService.getPersonIdentityDetailed(sessionItem.getSessionId());

            SignedJWT signedJWT =
                    verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                            sessionItem, personIdentity, kbvItem);
            auditService.sendAuditEvent(
                    AuditEventType.VC_ISSUED,
                    new AuditEventContext(input.getHeaders(), sessionItem),
                    verifiableCredentialService.getAuditEventExtensions(
                            kbvItem, sessionItem.getEvidenceRequest()));
            eventProbe.counterMetric(KBV_CREDENTIAL_ISSUER);
            auditService.sendAuditEvent(
                    AuditEventType.END, new AuditEventContext(input.getHeaders(), sessionItem));
            LOGGER.info(CREDENTIAL_ISSUED);
            return ApiGatewayResponseGenerator.proxyJwtResponse(
                    HttpStatusCode.OK, signedJWT.serialize());
        } catch (AwsServiceException ex) {
            eventProbe.log(ERROR, ex).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, ex.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, ex.awsErrorDetails().errorMessage());
        } catch (CredentialRequestException | ParseException | JOSEException e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.BAD_REQUEST, ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR);
        } catch (AccessTokenExpiredException e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, e.getMessage());
            return apiGatewayAccessDeniedError(ACCESS_TOKEN_EXPIRED);
        } catch (SessionExpiredException e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, e.getMessage());
            return apiGatewayAccessDeniedError(SESSION_EXPIRED);
        } catch (SessionNotFoundException e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, e.getMessage());
            return apiGatewayAccessDeniedError(SESSION_NOT_FOUND);
        } catch (NoSuchAlgorithmException e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, NO_SUCH_ALGORITHM_ERROR);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, NO_SUCH_ALGORITHM_ERROR);
        } catch (Exception e) {
            eventProbe.log(ERROR, e).counterMetric(KBV_CREDENTIAL_ISSUER, 0d);
            LOGGER.info(VC_MESSAGE_FORMAT, CREDENTIAL_NOT_ISSUED, e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            eventProbe.flush();
        }
    }

    private APIGatewayProxyResponseEvent apiGatewayAccessDeniedError(ErrorResponse errorType) {
        return ApiGatewayResponseGenerator.proxyJsonResponse(
                OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                OAuth2Error.ACCESS_DENIED
                        .appendDescription(" - " + errorType.getErrorSummary())
                        .toJSONObject());
    }

    private AccessToken validateInputHeaderBearerToken(Map<String, String> headers)
            throws CredentialRequestException, ParseException {
        var token =
                Optional.ofNullable(headers).stream()
                        .flatMap(x -> x.entrySet().stream())
                        .filter(
                                header ->
                                        AUTHORIZATION_HEADER_KEY.equalsIgnoreCase(header.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CredentialRequestException(
                                                ErrorResponse.MISSING_AUTHORIZATION_HEADER));

        return AccessToken.parse(token, AccessTokenType.BEARER);
    }
}
