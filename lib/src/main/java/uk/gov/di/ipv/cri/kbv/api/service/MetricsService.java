package uk.gov.di.ipv.cri.kbv.api.service;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;

import java.util.Map;
import java.util.Objects;

public class MetricsService {

    public static final String OUTCOME = "outcome";
    public static final String TRANS_ID = "transition_id";
    public static final String ERROR_CODE = "error_code";
    private final EventProbe eventProbe;

    public MetricsService(EventProbe eventProbe) {
        this.eventProbe = eventProbe;
    }

    public void sendErrorMetric(String errorCode, String metricName) {
        if (StringUtils.isNotBlank(errorCode)) {
            eventProbe.addDimensions(Map.of(ERROR_CODE, errorCode));
            eventProbe.counterMetric(metricName);
        }
    }

    public void sendResultMetric(KbvResult result, String metricName) {
        if (Objects.nonNull(result)) {
            String transIds = "";
            if (Objects.nonNull(result.getNextTransId()) && result.getNextTransId().length > 0) {
                transIds = String.join(",", result.getNextTransId());
            }
            if (StringUtils.isNotBlank(result.getOutcome())) {
                eventProbe.addDimensions(Map.of(OUTCOME, result.getOutcome(), TRANS_ID, transIds));
                eventProbe.counterMetric(metricName);
            }
        }
    }
}
