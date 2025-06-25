package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FailReport extends Report {
    private static final Logger LOGGER = LoggerFactory.getLogger(FailReport.class);

    public FailReport(Exception exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        this.addAttributes(
                "exception",
                Map.of("message", "%s: %s".formatted(exceptionType, exceptionMessage)));
        this.setPassed(false);

        LOGGER.error("Exception has been thrown while generating report", exception);
    }
}
