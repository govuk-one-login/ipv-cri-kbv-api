package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
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
    @Mock private APIGatewayProxyRequestEvent input;
    @Mock private KBVStorageService mockKbvStorageService;
    @Mock private SessionService mockSessionService;
    @InjectMocks private AbandonKbvHandler abandonKbvHandler;

    @Test
    void shouldReturn200OkWhenItReceivesAValidRequest() {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        when(input.getHeaders()).thenReturn(sessionHeader);

        KBVItem kbvItem = new KBVItem();
        when(mockKbvStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);
        SessionItem mockSessionItem = mock(SessionItem.class);
        when(mockSessionService.getSession(sessionHeader.get(HEADER_SESSION_ID)))
                .thenReturn(mockSessionItem);
        var result = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.OK, result.getStatusCode());
        assertEquals(ABANDON_STATUS, kbvItem.getStatus());

        verify(mockKbvStorageService)
                .getKBVItem(UUID.fromString(sessionHeader.get(HEADER_SESSION_ID)));
        verify(mockKbvStorageService).update(kbvItem);
        verify(mockSessionItem).setAuthorizationCode(any());
        verify(mockSessionService).createAuthorizationCode(mockSessionItem);
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }

    @Test
    void shouldReturnErrorBadRequestWhenItReceivesARequestThatDoesNotContainASessionId() {
        when(mockEventProbe.log(any(ERROR.getClass()), any(NullPointerException.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ABANDON_KBV, 0d)).thenReturn(mockEventProbe);

        var result = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.BAD_REQUEST, result.getStatusCode());
        verify(mockEventProbe).log(any(ERROR.getClass()), any(NullPointerException.class));
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }

    @Test
    void shouldReturn500InternalServerErrorWhenKbvItemCannotBeRetrievedDueToAWSError() {
        ArgumentCaptor<String> abandonStatusCapture = ArgumentCaptor.forClass(String.class);
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        when(input.getHeaders()).thenReturn(sessionHeader);

        var kbvItem = mock(KBVItem.class);

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS Server error occurred.")
                        .build();
        when(mockKbvStorageService.getKBVItem(
                        UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID))))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        when(mockEventProbe.log(any(ERROR.getClass()), any(AwsServiceException.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ABANDON_KBV, 0d)).thenReturn(mockEventProbe);

        verify(kbvItem, never()).setStatus(abandonStatusCapture.capture());

        var response = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotEquals(ABANDON_STATUS, kbvItem.getStatus());
        verify(mockKbvStorageService)
                .getKBVItem(UUID.fromString(input.getHeaders().get(HEADER_SESSION_ID)));
        verify(mockKbvStorageService, never()).update(kbvItem);
        verify(mockEventProbe).log(any(ERROR.getClass()), any(AwsServiceException.class));
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }

    @Test
    void shouldReturn500InternalServerErrorWhenSessionItemCannotBeRetrievedDueToAWSSError() {
        Map<String, String> sessionHeader = Map.of(HEADER_SESSION_ID, UUID.randomUUID().toString());
        when(input.getHeaders()).thenReturn(sessionHeader);
        var authorizationCode = UUID.randomUUID();
        var kbvItem = new KBVItem();
        var mockSessionItem = mock(SessionItem.class);
        when(mockKbvStorageService.getKBVItem(
                        UUID.fromString(sessionHeader.get(HEADER_SESSION_ID))))
                .thenReturn(kbvItem);
        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS Server error occurred.")
                        .build();

        when(mockSessionService.getSession(sessionHeader.get(HEADER_SESSION_ID)))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());
        when(mockEventProbe.log(any(ERROR.getClass()), any(AwsServiceException.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ABANDON_KBV, 0d)).thenReturn(mockEventProbe);

        var response = abandonKbvHandler.handleRequest(input, mock(Context.class));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());

        verify(mockSessionItem, never()).setAuthorizationCode(String.valueOf(authorizationCode));
        verify(mockSessionService).getSession(sessionHeader.get(HEADER_SESSION_ID));
        verify(mockSessionService, never()).createAuthorizationCode(mockSessionItem);
        verify(mockEventProbe).log(any(ERROR.getClass()), any(AwsServiceException.class));
        verify(mockEventProbe).counterMetric(ABANDON_KBV, 0d);
    }
}
