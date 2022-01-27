package uk.gov.di.ipv.cri.kbv.api.service;

public class ConfigurationService {

    private static ConfigurationService configurationService;

    public static ConfigurationService getInstance() {
        if (configurationService == null) {
            configurationService = new ConfigurationService();
        }
        return configurationService;
    }

    public boolean isRunningLocally() {
        return Boolean.parseBoolean(System.getenv("IS_LOCAL"));
    }

    public String getKBVSessionTableName() {
        return System.getenv("KBV_SESSION_TABLE_NAME");
    }
}
