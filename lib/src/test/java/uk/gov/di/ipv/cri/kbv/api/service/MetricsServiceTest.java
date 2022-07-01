package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.ERROR_CODE;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.OUTCOME;
import static uk.gov.di.ipv.cri.kbv.api.service.MetricsService.TRANS_ID;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private EventProbe eventProbe;

    @Mock Results results;

    @Mock Error error;

    @Mock ArrayOfString arrayOfString;

    @Test
    void shouldSendResultsMetric() {
        MetricsService metricsService = new MetricsService(eventProbe);

        when(results.getOutcome()).thenReturn("foo");
        when(results.getNextTransId()).thenReturn(arrayOfString);
        when(arrayOfString.getString()).thenReturn(List.of("bar"));
        metricsService.sendResultMetric(results, "baz");
        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(OUTCOME, "foo", TRANS_ID, "bar"));
    }

    @Test
    void shouldSendErrorMetric() {
        MetricsService metricsService = new MetricsService(eventProbe);
        when(error.getErrorCode()).thenReturn("oh no!");
        metricsService.sendErrorMetric(error, "baz");
        verify(eventProbe).counterMetric("baz");
        verify(eventProbe).addDimensions(Map.of(ERROR_CODE, "oh no!"));
    }
}
