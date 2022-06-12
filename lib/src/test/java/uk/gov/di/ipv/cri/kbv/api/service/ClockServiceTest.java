package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClockServiceTest {
    private static final String STACK_NAME = "stack-name";
    @Mock private SSMProvider mockSSMProvider;
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    ArgumentCaptor<String> capturedSecretKey = ArgumentCaptor.forClass(String.class);

    private ClockService clockService;
    private long epochSeconds = clock.instant().getEpochSecond();
    private AWSParamStoreRetriever awsParamStoreRetriever;

    @BeforeEach
    void setUp() {
        awsParamStoreRetriever = new AWSParamStoreRetriever(mockSSMProvider, STACK_NAME);
        this.clockService = new ClockService(awsParamStoreRetriever, clock);
    }

    @Test
    void shouldThrowErrorWhenAWSSecretsRetrieverIsNull() {
        NullPointerException expectedException =
                assertThrows(NullPointerException.class, () -> new ClockService(null, clock));

        assertEquals("awsParamStoreRetriever must not be null", expectedException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenClockIsNull() {
        NullPointerException expectedException =
                assertThrows(
                        NullPointerException.class,
                        () -> new ClockService(awsParamStoreRetriever, null));

        assertEquals("clock must not be null", expectedException.getMessage());
    }

    @Test
    void shouldReturnTheCurrentClockEpochSecond() {
        when(mockSSMProvider.get(capturedSecretKey.capture()))
                .thenReturn(String.valueOf(epochSeconds));
        assertEquals(
                clockService.getEpochSecond(),
                clock.instant().plus(epochSeconds, ChronoUnit.SECONDS).getEpochSecond());
    }
}
