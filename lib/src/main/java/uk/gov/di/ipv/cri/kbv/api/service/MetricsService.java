package uk.gov.di.ipv.cri.kbv.api.service;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;

import java.util.Map;
import java.util.Objects;

public class MetricsService {

    public static final String OUTCOME = "outcome";
    public static final String ERROR_CODE = "error_code";

    private final EventProbe eventProbe;

    public MetricsService(EventProbe eventProbe) {
        this.eventProbe = eventProbe;
    }

    public void sendDurationMetric(String durationMetricName, long executionDuration) {
        eventProbe.counterMetric(
                EventProbe.clean(durationMetricName), executionDuration, MetricUnit.MILLISECONDS);
    }

    public void sendErrorMetric(String metricName, String errorCode) {
        if (StringUtils.isNotBlank(errorCode)) {
            eventProbe.addDimensions(Map.of(ERROR_CODE, EventProbe.clean(errorCode)));
            eventProbe.counterMetric(EventProbe.clean(metricName));
        }
    }

    public void sendResultMetric(String metricName, KbvResult result) {
        if (Objects.nonNull(result) && StringUtils.isNotBlank(result.getOutcome())) {
            eventProbe.addDimensions(Map.of(OUTCOME, EventProbe.clean(result.getOutcome())));
            eventProbe.counterMetric(EventProbe.clean(metricName));
        }
    }

    public EventProbe getEventProbe() {
        return eventProbe;
    }
}
