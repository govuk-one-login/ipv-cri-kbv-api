package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions;

import java.util.Map;

public class FailReport extends Report {
    public FailReport(Exception exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        this.addAttributes(
                "exception",
                Map.of("message", "%s: %s".formatted(exceptionType, exceptionMessage)));
        this.setPassed(false);
    }
}
