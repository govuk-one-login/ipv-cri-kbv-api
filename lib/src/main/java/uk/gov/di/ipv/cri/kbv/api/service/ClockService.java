package uk.gov.di.ipv.cri.kbv.api.service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static uk.gov.di.ipv.cri.kbv.api.config.ConfigurationConstants.SESSION_TTL;

public class ClockService {
    private final AWSParamStoreRetriever awsParamStoreRetriever;
    private final Clock clock;

    public ClockService() {
        this(new AWSParamStoreRetriever(), Clock.systemUTC());
    }

    public ClockService(AWSParamStoreRetriever awsParamStoreRetriever, Clock clock) {
        this.awsParamStoreRetriever =
                Objects.requireNonNull(
                        awsParamStoreRetriever, "awsParamStoreRetriever must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public long getEpochSecond() {
        return clock.instant()
                .plus(
                        Long.parseLong(awsParamStoreRetriever.getValue(SESSION_TTL)),
                        ChronoUnit.SECONDS)
                .getEpochSecond();
    }
}
