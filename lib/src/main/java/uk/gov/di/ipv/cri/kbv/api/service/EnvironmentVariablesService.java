package uk.gov.di.ipv.cri.kbv.api.service;

import java.util.Optional;

public class EnvironmentVariablesService {

    public static final String GENDER = "GENDER";

    public Optional<String> getEnvironmentVariable(String environmentVariable) {
        return Optional.ofNullable(System.getenv(environmentVariable));
    }
}
