package uk.gov.di.ipv.cri.kbv.healthcheck.util.bash;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class BashCommandConfiguration {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private final Duration timeout;
    private final Map<String, String> environmentVariables;

    public BashCommandConfiguration(Duration timeout, Map<String, String> environment) {
        this.timeout = timeout;
        this.environmentVariables = environment;
    }

    public static BashCommandConfiguration defaultConfig() {
        return new BashCommandConfiguration(DEFAULT_TIMEOUT, Collections.emptyMap());
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
}
