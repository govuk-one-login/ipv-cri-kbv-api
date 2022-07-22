package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.ERROR_CODE;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.OUTCOME;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.TRANS_ID;

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

        this.metricsService.sendResultMetric(kbvResult, "baz");

        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(OUTCOME, resultOutcome, TRANS_ID, resultTransId));
    }

    @Test
    void shouldSendErrorMetric() {
        String errorCode = "error-code";
        this.metricsService.sendErrorMetric(errorCode, "baz");
        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(ERROR_CODE, errorCode));
    }
}
