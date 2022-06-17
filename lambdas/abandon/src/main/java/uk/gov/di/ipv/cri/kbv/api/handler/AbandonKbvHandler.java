package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;

public class AbandonKbvHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String ABANDON_STATUS = "Abandoned";

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String ABANDON_KBV = "abandon_kbv";
    private final EventProbe eventProbe;
    private final KBVStorageService kbvStorageService;

    public AbandonKbvHandler(EventProbe eventProbe, KBVStorageService kbvStorageService) {
        this.eventProbe = eventProbe;
        this.kbvStorageService = kbvStorageService;
    }

    public AbandonKbvHandler() {
        this.eventProbe = new EventProbe();
        this.kbvStorageService = new KBVStorageService(new ConfigurationService());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.withHeaders(Map.of("Content-Type", "application/json"));
        try {
            UUID sessionId = UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID));
            KBVItem kbvItem = kbvStorageService.getKBVItem(sessionId);
            kbvItem.setStatus(ABANDON_STATUS);
            kbvStorageService.update(kbvItem);
            response.withStatusCode(HttpStatusCode.OK);
            eventProbe.counterMetric(ABANDON_KBV, 0d);
            return response;
        } catch (NullPointerException npe) {
            response.withStatusCode(HttpStatusCode.BAD_REQUEST);
            eventProbe.log(ERROR, npe).counterMetric(ABANDON_KBV, 0d);
        }
        return response;
    }
}
