package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;

import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.AbandonKbvHandler.ABANDON_KBV;
import static uk.gov.di.ipv.cri.kbv.api.handler.AbandonKbvHandler.ABANDON_STATUS;
import static uk.gov.di.ipv.cri.kbv.api.handler.AbandonKbvHandler.HEADER_SESSION_ID;

@ExtendWith(MockitoExtension.class)
class AbandonKbvHandlerTest {
    @Mock private EventProbe mockEventProbe;
    @InjectMocks private AbandonKbvHandler abandonKbvHandler;
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private KBVStorageService mockKbvStorageService;

    @Test
    void shouldReturn200OkWhenItReceivesAValidRequest() {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());

        when(input.getHeaders()).thenReturn(sessionHeader);
        KBVItem kbvItem = new KBVItem();
        when(mockKbvStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);
        var result = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertEquals(kbvItem.getStatus(), ABANDON_STATUS);
        verify(mockKbvStorageService).update(kbvItem);
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }

    @Test
    void shouldReturnErrorBadRequestWhenItReceivesARequestThatDoesNotContainASessionId() {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        when(mockEventProbe.log(any(ERROR.getClass()), any(NullPointerException.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ABANDON_KBV, 0d)).thenReturn(mockEventProbe);

        var result = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.BAD_REQUEST, result.getStatusCode());
        verify(mockEventProbe).log(any(ERROR.getClass()), any(NullPointerException.class));
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }

    @Test
    void shouldReturn400BadRequestWhenKbvItemStatusIsNotSetToAbandon() {
        ArgumentCaptor<String> abandonStatusCapture = ArgumentCaptor.forClass(String.class);

        KBVItem kbvItem = mock(KBVItem.class);

        when(mockEventProbe.log(any(ERROR.getClass()), any(NullPointerException.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ABANDON_KBV, 0d)).thenReturn(mockEventProbe);

        verify(kbvItem, never()).setStatus(abandonStatusCapture.capture());

        var result = abandonKbvHandler.handleRequest(input, mock(Context.class));
        assertNotEquals(kbvItem.getStatus(), ABANDON_STATUS);
        assertEquals(HttpStatusCode.BAD_REQUEST, result.getStatusCode());
        verify(mockEventProbe).log(any(ERROR.getClass()), any(NullPointerException.class));
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }
}
