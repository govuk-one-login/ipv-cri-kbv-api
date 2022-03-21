package uk.gov.di.ipv.cri.kbv.api.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.cri.kbv.api.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.util.Map;
import java.util.Objects;

public class ConfigurationService {

    private static ConfigurationService configurationService;
    private final SSMProvider ssmProvider;
    private final String parameterPrefix;

    public static ConfigurationService getInstance() {
        if (configurationService == null) {
            configurationService = new ConfigurationService();
        }
        return configurationService;
    }

    ConfigurationService() {
        this.ssmProvider = ParamManager.getSsmProvider();
        this.parameterPrefix =
                Objects.requireNonNull(
                        System.getenv("AWS_STACK_NAME"), "env var AWS_STACK_NAME required");
    }

    @ExcludeFromGeneratedCoverageReport
    public ConfigurationService(SSMProvider ssmProvider, String parameterPrefix) {
        this.ssmProvider = ssmProvider;
        this.parameterPrefix = parameterPrefix;
    }

    public boolean isRunningLocally() {
        return Boolean.parseBoolean(System.getenv("IS_LOCAL"));
    }

    public String getKBVSessionTableName() {
        return System.getenv("KBV_SESSION_TABLE_NAME");
    }

    public Map<String, String> getParametersForPath(String path) {
        String format = String.format("/%s/%s", parameterPrefix, path);
        return ssmProvider.recursive().getMultiple(format.replace("//", "/"));
    }
}
