package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricsService {

    public static final String OUTCOME = "outcome";
    public static final String TRANS_ID = "transId";
    public static final String ERROR_CODE = "error_code";
    private final EventProbe eventProbe;

    public MetricsService(EventProbe eventProbe) {
        this.eventProbe = eventProbe;
    }

    public void sendErrorMetric(Error error, String metricName) {
        if (error != null && error.getErrorCode() != null) {
            String errorCode = error.getErrorCode();
            eventProbe.addDimensions(Map.of(ERROR_CODE, errorCode));
            eventProbe.counterMetric(metricName);
        }
    }

    public void sendResultMetric(Results results, String metricName) {
        if (results != null) {
            String outcome = results.getOutcome();
            ArrayOfString nextTransId = results.getNextTransId();
            String transIds = "";
            if (nextTransId != null) {
                List<String> transId = nextTransId.getString();
                if (transId != null) {
                    transIds = transId.stream().collect(Collectors.joining(","));
                }
            }

            if (outcome != null && !outcome.isBlank()) {
                eventProbe.addDimensions(Map.of(OUTCOME, outcome, TRANS_ID, transIds));
                eventProbe.counterMetric(metricName);
            }
        }
    }
}
