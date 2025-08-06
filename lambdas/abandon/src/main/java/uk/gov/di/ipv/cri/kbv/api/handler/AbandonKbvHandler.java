package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.parameters.AppConfigProvider;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.ServiceFactory;

import java.util.Map;

public class AbandonKbvHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String ABANDON_STATUS = "Abandoned";
    private static final String HEADER_SESSION_ID = "session-id";
    private static final String ABANDON_KBV = "abandon_kbv";
    private final EventProbe eventProbe;
    private final AuditService auditService;

    private final KBVStorageService kbvStorageService;
    private final SessionService sessionService;

    public AbandonKbvHandler(
            EventProbe eventProbe,
            KBVStorageService kbvStorageService,
            SessionService sessionService,
            AuditService auditService) {
        this.eventProbe = eventProbe;
        this.kbvStorageService = kbvStorageService;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @ExcludeFromGeneratedCoverageReport
    public AbandonKbvHandler() {
        ServiceFactory serviceFactory = new ServiceFactory();
        this.eventProbe = new EventProbe();
        this.kbvStorageService =
                new KBVStorageService(
                        serviceFactory.getConfigurationService(),
                        serviceFactory.getDynamoDbEnhancedClient());
        this.sessionService = serviceFactory.getSessionService();
        this.auditService = serviceFactory.getAuditService();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        var applicationId = System.getenv("APP_CONFIG_ID");
        var environmentId = System.getenv("APP_CONFIG_ENVIRONMENT_ID");
        var profileId = System.getenv("APP_CONFIG_PROFILE_ID");

        System.out.println("hi");
        System.out.println(applicationId);
        System.out.println(environmentId);

        AppConfigProvider appConfigProvider =
                ParamManager.getAppConfigProvider(environmentId, applicationId);
        // .withMaxAge(3, MINUTES);

        String paramsRaw = appConfigProvider.get(profileId);

        System.out.println();
        System.out.println("paramsRaw: " + paramsRaw);

        response.withHeaders(Map.of("Content-Type", "application/json"));
        // try {
        //     var sessionHeader = input.getHeaders().get(HEADER_SESSION_ID);
        //     var sessionId = UUID.fromString(sessionHeader);
        //     var kbvItem = kbvStorageService.getKBVItem(sessionId);
        //     kbvItem.setStatus(ABANDON_STATUS);
        //     kbvStorageService.update(kbvItem);

        //     var sessionItem = sessionService.validateSessionId(sessionId.toString());
        //     sessionService.createAuthorizationCode(sessionItem);

        //     response.withStatusCode(HttpStatusCode.OK);
        //     eventProbe.counterMetric(ABANDON_KBV);

        //     auditService.sendAuditEvent(
        //             ABANDONED.toString(), new AuditEventContext(input.getHeaders(),
        // sessionItem));

        //     return response;

        // } catch (NullPointerException | SqsException npe) {
        //     response.withStatusCode(HttpStatusCode.BAD_REQUEST);
        //     eventProbe.log(ERROR, npe).counterMetric(ABANDON_KBV, 0d);
        // } catch (AwsServiceException e) {
        //     response.withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR);
        //     eventProbe.log(ERROR, e).counterMetric(ABANDON_KBV, 0d);
        // }
        // return response;

        return response.withStatusCode(HttpStatusCode.OK);
    }
}
