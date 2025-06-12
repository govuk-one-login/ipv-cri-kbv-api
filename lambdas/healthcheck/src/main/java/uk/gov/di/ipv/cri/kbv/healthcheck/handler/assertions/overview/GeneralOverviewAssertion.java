package uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.overview;

import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Assertion;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.assertions.Report;

import java.util.Map;

public class GeneralOverviewAssertion implements Assertion {
    private final Map<String, Report> reports;

    public GeneralOverviewAssertion(Map<String, Report> reports) {
        this.reports = reports;
    }

    @Override
    public Report run() {
        Report report = new Report();
        boolean allPassed = true;

        for (var entry : reports.entrySet()) {
            String key = entry.getKey();
            boolean passed = entry.getValue().isPassed();

            report.addAttributes(key, Map.of("passed", passed));

            if (!passed) {
                allPassed = false;
            }
        }

        report.setPassed(allPassed);
        return report;
    }
}
