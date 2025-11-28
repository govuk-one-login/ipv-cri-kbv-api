package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.ERROR_CODE;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.OUTCOME;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private EventProbe eventProbe;

    @InjectMocks private MetricsService metricsService;

    @Test
    void shouldSendResultsMetric() {
        String resultOutcome = "outcome";
        String resultTransId = "transId";
        KbvResult kbvResult = new KbvResult();
        kbvResult.setOutcome(resultOutcome);
        kbvResult.setNextTransId(new String[] {resultTransId});

        this.metricsService.sendResultMetric("baz", kbvResult);

        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(OUTCOME, resultOutcome));
    }

    @Test
    void shouldNotSendResultsMetricWithoutResult() {
        this.metricsService.sendResultMetric("baz", null);

        verify(eventProbe, never()).counterMetric("baz");
        verify(eventProbe, never()).addDimensions(Map.of(OUTCOME, anyString()));
    }

    @Test
    void shouldNotSendResultsMetricWithoutResultOutcome() {
        KbvResult kbvResult = new KbvResult();

        this.metricsService.sendResultMetric("baz", kbvResult);

        verify(eventProbe, never()).counterMetric("baz");
        verify(eventProbe, never()).addDimensions(Map.of(OUTCOME, anyString()));
    }

    @Test
    void shouldSendDurationMetric() {
        long executionDuration = 7500;

        this.metricsService.sendDurationMetric("baz_duration", executionDuration);

        verify(eventProbe).counterMetric("baz_duration", executionDuration, Unit.MILLISECONDS);
    }

    @Test
    void shouldSendErrorMetric() {
        String errorCode = "error-code";

        this.metricsService.sendErrorMetric("baz", errorCode);

        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(ERROR_CODE, errorCode));
    }

    @Test
    void shouldNotSendErrorMetric() {
        String errorCode = "";

        this.metricsService.sendErrorMetric("baz", errorCode);

        verify(eventProbe, never()).counterMetric("baz");
        verify(eventProbe, never()).addDimensions(Map.of(ERROR_CODE, errorCode));
    }

    @Test
    void eventProbeShouldNotBeNull() {
        assertNotNull(metricsService.getEventProbe());
    }
}
