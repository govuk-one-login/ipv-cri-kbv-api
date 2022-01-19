package uk.gov.di.cri.experian.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;

public class ValidationResult {
    private final List<String> errors;
    private final boolean isValid;

    public ValidationResult(List<String> errors) {
        this.isValid = Objects.isNull(errors) || errors.isEmpty();
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    @JsonIgnore
    public boolean isValid() {
        return isValid;
    }
}
