package gov.uk.kbv.api.stepdefinitions;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.uk.kbv.api.client.KbvApiClient;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.CommonApiClient;
import uk.gov.di.ipv.cri.common.library.client.TestResourcesClient;
import uk.gov.di.ipv.cri.common.library.config.Environment;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThinFileAlarmStepDefinition {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int ALARM_PERIOD_IN_SECONDS = 900;

    private static final Duration ALARM_STATE_TIMEOUT =
            Duration.ofSeconds(ALARM_PERIOD_IN_SECONDS).plusMinutes(5);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(30);
    private static final Duration METRIC_INGESTION_ALLOWANCE = Duration.ofMinutes(2);

    private static final String THIN_FILE_CLAIM_OVERRIDES = "ERROR_INSUFFICIENT_QUESTIONS.json";
    private static final String THIN_FILE_METRIC_ID = "thinFiles";
    private static final String RESPONSES_METRIC_ID = "responses";

    private static final CloudWatchClient CLOUD_WATCH =
            CloudWatchClient.builder().region(Region.EU_WEST_2).build();

    private final TestResourcesClient testResourcesClient;
    private final CommonApiClient commonApiClient;
    private final KbvApiClient kbvApiClient;

    private String alarmName;

    public ThinFileAlarmStepDefinition(
            ClientConfigurationService clientConfigurationService, SSMHelper ssmHelper) {
        this.testResourcesClient = new TestResourcesClient(clientConfigurationService);
        this.commonApiClient = new CommonApiClient(clientConfigurationService, ssmHelper);
        this.kbvApiClient = new KbvApiClient(clientConfigurationService);
    }

    @Given("the thin file rate alarm exists and is not already firing")
    public void theThinFileRateAlarmExistsAndIsNotAlreadyFiring() {
        alarmName =
                Environment.getEnv("STACK_NAME")
                        + "-%s-".formatted(Environment.getEnv("env"))
                        + "ExperianKBVThinFileRateAlarm";

        assertNotEquals(
                StateValue.ALARM,
                describeAlarm().stateValue(),
                alarmName + " is already in the ALARM state, so this test cannot prove anything");
    }

    @When("{int} thin file journeys are completed")
    public void thinFileJourneysAreCompleted(int journeys)
            throws IOException, InterruptedException {
        completeThinFileJourneys(journeys);
        sleepFor(METRIC_INGESTION_ALLOWANCE);
    }

    @When("{int} thin file journeys are completed in each of {int} consecutive alarm periods")
    public void thinFileJourneysAreCompletedInConsecutivePeriods(int journeys, int periods)
            throws IOException, InterruptedException {
        for (int period = 0; period < periods; period++) {
            waitForStartOfNextAlarmPeriod();
            completeThinFileJourneys(journeys);
        }
    }

    @Then("the alarm's metrics show at least {int} thin files out of at least {int} responses")
    public void theAlarmsMetricsShowThinFiles(int thinFiles, int responses) {
        Map<String, List<Double>> metrics = getAlarmMetricData();

        assertTrue(
                sumOf(metrics, THIN_FILE_METRIC_ID) >= thinFiles,
                "Expected at least "
                        + thinFiles
                        + " thin files but the alarm's metrics reported "
                        + sumOf(metrics, THIN_FILE_METRIC_ID));
        assertTrue(
                sumOf(metrics, RESPONSES_METRIC_ID) >= responses,
                "Expected at least "
                        + responses
                        + " responses but the alarm's metrics reported "
                        + sumOf(metrics, RESPONSES_METRIC_ID));
    }

    @Then("the thin file rate alarm enters the ALARM state")
    public void theThinFileRateAlarmEntersTheAlarmState() throws InterruptedException {
        Instant giveUpAt = Instant.now().plus(ALARM_STATE_TIMEOUT);

        while (Instant.now().isBefore(giveUpAt)) {
            MetricAlarm alarm = describeAlarm();

            if (StateValue.ALARM.equals(alarm.stateValue())) {
                LOGGER.info("{} entered the ALARM state: {}", alarmName, alarm.stateReason());
                return;
            }

            LOGGER.info("{} is {}, waiting", alarmName, alarm.stateValue());
            sleepFor(POLL_INTERVAL);
        }

        Map<String, List<Double>> metrics = getAlarmMetricData();
        throw new AssertionError(
                alarmName
                        + " did not enter the ALARM state within "
                        + ALARM_STATE_TIMEOUT
                        + ". The alarm's metrics reported "
                        + sumOf(metrics, THIN_FILE_METRIC_ID)
                        + " thin files out of "
                        + sumOf(metrics, RESPONSES_METRIC_ID)
                        + " responses, so another journey in this account may have returned"
                        + " questions and kept the thin file rate below 100%.");
    }

    @After("@thin_file_alarm")
    public void returnTheAlarmToTheOkState() throws IOException, InterruptedException {
        if (alarmName == null || !StateValue.ALARM.equals(describeAlarm().stateValue())) {
            return;
        }

        LOGGER.info("Completing a journey that returns questions to clear {}", alarmName);
        completeJourneyReturningQuestions();

        Instant giveUpAt = Instant.now().plus(ALARM_STATE_TIMEOUT);

        while (Instant.now().isBefore(giveUpAt)) {
            StateValue state = describeAlarm().stateValue();

            if (StateValue.OK.equals(state)) {
                LOGGER.info("{} returned to the OK state", alarmName);
                return;
            }

            LOGGER.info("{} is {}, waiting for it to clear", alarmName, state);
            sleepFor(POLL_INTERVAL);
        }

        LOGGER.warn(
                "{} was still not OK after {}. It clears on its own once both breaching data points"
                        + " age out of the evaluation range, unless something else in this account"
                        + " is still returning thin files.",
                alarmName,
                ALARM_STATE_TIMEOUT);
    }

    private void completeThinFileJourneys(int journeys) throws IOException, InterruptedException {
        for (int journey = 1; journey <= journeys; journey++) {
            HttpResponse<String> startResponse =
                    testResourcesClient.sendOverwrittenStartRequest(THIN_FILE_CLAIM_OVERRIDES);
            assertEquals(200, startResponse.statusCode());

            String sessionId = createSession(startResponse.body());

            assertEquals(
                    204,
                    kbvApiClient.sendQuestionRequest(sessionId).statusCode(),
                    "Expected no questions to be returned for a thin file user");

            LOGGER.info("Completed thin file journey {} of {}", journey, journeys);
        }
    }

    private void completeJourneyReturningQuestions() throws IOException, InterruptedException {
        HttpResponse<String> startResponse = testResourcesClient.sendStartRequest();
        assertEquals(200, startResponse.statusCode());

        assertEquals(
                200,
                kbvApiClient.sendQuestionRequest(createSession(startResponse.body())).statusCode(),
                "Expected the default test user to be asked a question");
    }

    private String createSession(String sessionRequestBody)
            throws IOException, InterruptedException {
        HttpResponse<String> sessionResponse =
                commonApiClient.sendSessionRequest(sessionRequestBody);
        String sessionId =
                OBJECT_MAPPER.readTree(sessionResponse.body()).path("session_id").asText(null);
        assertNotNull(sessionId, "No session-id returned: " + sessionResponse.body());
        return sessionId;
    }

    private Map<String, List<Double>> getAlarmMetricData() {
        List<MetricDataQuery> queries =
                describeAlarm().metrics().stream()
                        .map(query -> query.toBuilder().returnData(true).build())
                        .collect(Collectors.toList());

        GetMetricDataResponse response =
                CLOUD_WATCH.getMetricData(
                        request ->
                                request.metricDataQueries(queries)
                                        .startTime(
                                                Instant.now()
                                                        .minusSeconds(2L * ALARM_PERIOD_IN_SECONDS))
                                        .endTime(Instant.now()));

        return response.metricDataResults().stream()
                .collect(
                        Collectors.toMap(
                                MetricDataResult::id, MetricDataResult::values, (a, b) -> a));
    }

    private double sumOf(Map<String, List<Double>> metrics, String metricId) {
        return metrics.getOrDefault(metricId, List.of()).stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private MetricAlarm describeAlarm() {
        return CLOUD_WATCH
                .describeAlarms(request -> request.alarmNames(alarmName))
                .metricAlarms()
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find the alarm " + alarmName));
    }

    private void waitForStartOfNextAlarmPeriod() throws InterruptedException {
        long secondsIntoPeriod = Instant.now().getEpochSecond() % ALARM_PERIOD_IN_SECONDS;
        Duration untilNextPeriod =
                Duration.ofSeconds(ALARM_PERIOD_IN_SECONDS - secondsIntoPeriod).plusSeconds(5);

        LOGGER.info("Waiting {} for the start of the next alarm period", untilNextPeriod);
        sleepFor(untilNextPeriod);
    }

    @SuppressWarnings("java:S2925")
    private void sleepFor(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }
}
