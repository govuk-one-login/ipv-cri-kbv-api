package uk.gov.di.ipv.cri.kbv.api.models;

public class TestResult {
    private LambdaReport lambdaReport;

    public TestResult(LambdaReport lambdaReport) {
        this.lambdaReport = lambdaReport;
    }

    public LambdaReport getLambdaReport() {
        return lambdaReport;
    }

    public void setLambdaReport(LambdaReport lambdaReport) {
        this.lambdaReport = lambdaReport;
    }
}
