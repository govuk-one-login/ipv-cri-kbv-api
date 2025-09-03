package gov.uk.kbv.api.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionResponse;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryRequest;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryResponse;
import software.amazon.awssdk.services.sfn.model.HistoryEventType;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;
import uk.gov.di.ipv.cri.common.library.aws.CloudFormationHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CertificateExpiryStepDefinition {

    private static final SfnClient SFN_CLIENT =
            SfnClient.builder().region(Region.EU_WEST_2).build();
    private static final ZonedDateTime NOW = ZonedDateTime.now(ZoneOffset.UTC);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
    private static final String DUMMY_CERT_JSON =
            """
                    {
                      "KeyStoreEntriesOverride": [
                        {
                          "aliasName": "test-alias",
                          "certificates": [
                            {
                              "owner": "CN=GOVUKONELOGINTESTCERT, OU=Environment, OU=OwnerA, O=Issuer, C=US",
                              "serialNumber": "aaaaaaaa",
                              "validFrom": "Wed Jan 01 00:00:00 UTC 2025 until: %s",
                              "version": 3,
                              "issuer": "CN=Issuer1, CN=Issuer2, CN=Issuer3, CN=Issuer4, CN=Issuer5, DC=DomainController1, DC=DomainController2",
                              "fingerprints": {
                                "SHA256": "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:A****************************************************************",
                                "SHA1": "AA:AA:AA:AA:AA:AA:A****************************************"
                              }
                            }
                          ],
                          "creationDate": "Jan 01, 2025"
                        }
                      ]
                    }
                    """;
    private static final String THREE_CERTS_JSON =
            """
                    {
                      "KeyStoreEntriesOverride": [
                        {
                          "aliasName": "test-alias",
                          "certificates": [
                            {
                              "owner": "CN=GOVUKONELOGINTESTCERT, OU=Environment, OU=OwnerA, O=Issuer, C=US",
                              "serialNumber": "aaaaaaaa",
                              "validFrom": "Wed Jan 01 00:00:00 UTC 2025 until: %s",
                              "version": 3,
                              "issuer": "CN=Issuer1, CN=Issuer2, CN=Issuer3, CN=Issuer4, CN=Issuer5, DC=DomainController1, DC=DomainController2",
                              "fingerprints": {
                                "SHA256": "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:A****************************************************************",
                                "SHA1": "AA:AA:AA:AA:AA:AA:A****************************************"
                              }
                            },
                            {
                              "owner": "CN=GOVUKONELOGINTESTCERT, OU=Environment, OU=OwnerA, O=Issuer, C=US",
                              "serialNumber": "aaaaaaaa",
                              "validFrom": "Wed Jan 01 00:00:00 UTC 2025 until: %s",
                              "version": 3,
                              "issuer": "CN=Issuer1, CN=Issuer2, CN=Issuer3, CN=Issuer4, CN=Issuer5, DC=DomainController1, DC=DomainController2",
                              "fingerprints": {
                                "SHA256": "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:A****************************************************************",
                                "SHA1": "AA:AA:AA:AA:AA:AA:A****************************************"
                              }
                            }
                          ],
                          "creationDate": "Jan 01, 2025"
                        },
                        {
                          "aliasName": "test-alias",
                          "certificates": [
                            {
                              "owner": "CN=GOVUKONELOGINTESTCERT, OU=Environment, OU=OwnerA, O=Issuer, C=US",
                              "serialNumber": "aaaaaaaa",
                              "validFrom": "Wed Jan 01 00:00:00 UTC 2025 until: %s",
                              "version": 3,
                              "issuer": "CN=Issuer1, CN=Issuer2, CN=Issuer3, CN=Issuer4, CN=Issuer5, DC=DomainController1, DC=DomainController2",
                              "fingerprints": {
                                "SHA256": "AA:AA:AA:AA:AA:AA:AA:AA:AA:AA:A****************************************************************",
                                "SHA1": "AA:AA:AA:AA:AA:AA:A****************************************"
                              }
                            }
                          ],
                          "creationDate": "Jan 01, 2025"
                        }
                      ]
                    }
                    """;

    private String stateMachineInput;
    private String executionArn;

    @Given("no certificate overrides are passed into the step function")
    public void noCertificateOverridesArePassedIntoTheStepFunction() {
        stateMachineInput = "{}";
    }

    @Given("no certificates are expiring soon")
    public void noCertificatesAreExpiringSoon() {
        stateMachineInput = String.format(DUMMY_CERT_JSON, NOW.plusYears(1).format(FORMATTER));
    }

    @Given("a certificate is expiring within 90 days")
    public void aCertificateIsExpiringWithin90Days() {
        stateMachineInput = String.format(DUMMY_CERT_JSON, NOW.plusDays(50).format(FORMATTER));
    }

    @Given("a certificate is expiring within 7 days")
    public void aCertificateIsExpiringWithin7Days() {
        stateMachineInput = String.format(DUMMY_CERT_JSON, NOW.plusDays(1).format(FORMATTER));
    }

    @Given("three certificates with various expiries are passed into the step function")
    public void threeCertificatesArePassedIntoTheStepFunction() {
        stateMachineInput =
                String.format(
                        THREE_CERTS_JSON,
                        NOW.plusYears(1).format(FORMATTER),
                        NOW.plusDays(80).format(FORMATTER),
                        NOW.plusDays(5).format(FORMATTER));
    }

    @When("the step function is invoked")
    public void theStepFunctionIsInvoked() {
        String certificateExpiryStepFunctionArn =
                CloudFormationHelper.getOutput(
                        System.getenv("STACK_NAME"), "CertificateExpiryStepFunctionArn");

        StartExecutionResponse response =
                SFN_CLIENT.startExecution(
                        StartExecutionRequest.builder()
                                .stateMachineArn(certificateExpiryStepFunctionArn)
                                .name(
                                        String.join(
                                                "-",
                                                "integration-test",
                                                UUID.randomUUID().toString()))
                                .input(stateMachineInput)
                                .build());
        executionArn = response.executionArn();
    }

    @SuppressWarnings("java:S2925")
    private ExecutionStatus pollForExecutionStatus() throws InterruptedException {
        for (int count = 0; count < 20; count++) {
            DescribeExecutionResponse executionOutcome =
                    SFN_CLIENT.describeExecution(
                            DescribeExecutionRequest.builder().executionArn(executionArn).build());

            if (!executionOutcome.status().equals(ExecutionStatus.RUNNING)) {
                return executionOutcome.status();
            }

            Thread.sleep(2000);
        }

        return ExecutionStatus.RUNNING;
    }

    @Then("the step function executes successfully")
    public void theStepFunctionExecutesSuccessfully() throws InterruptedException {
        ExecutionStatus executionStatus = pollForExecutionStatus();
        assertEquals(ExecutionStatus.SUCCEEDED, executionStatus);
    }

    private String buildStateName(int metricValue, int metricType) {
        String metricNameComponent = "LessThan" + metricType + "Days metric";

        if (metricValue == 0) {
            return "Push zero to " + metricNameComponent;
        }
        return "Fire " + metricNameComponent;
    }

    @Then("the step function will push {int} to the {int} day metric")
    public void theStepFunctionWillPushTheDayMetric(int metricValue, int metricType) {
        GetExecutionHistoryResponse executionHistoryResponse =
                SFN_CLIENT.getExecutionHistory(
                        GetExecutionHistoryRequest.builder()
                                .executionArn(executionArn)
                                .reverseOrder(true)
                                .maxResults(1000)
                                .build());

        String stateName = buildStateName(metricValue, metricType);

        boolean hasTaskCompleted =
                executionHistoryResponse.events().stream()
                        .anyMatch(
                                event ->
                                        event.type().equals(HistoryEventType.TASK_STATE_EXITED)
                                                && event.stateExitedEventDetails()
                                                        .name()
                                                        .equals(stateName));

        assertTrue(hasTaskCompleted);
    }
}
