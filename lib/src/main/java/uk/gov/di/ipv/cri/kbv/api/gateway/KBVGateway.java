package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;
import uk.gov.di.ipv.cri.kbv.api.util.OpenTelemetryUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class KBVGateway {
    private static final String EXPERIAN_IIQ_REQUEST = "experian_iiq_request_type";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String EXPERIAN_INITIAL_QUESTION_RESPONSE = "initial_questions_response";
    private static final String EXPERIAN_SUBMIT_RESPONSE = "submit_questions_response";

    private final StartAuthnAttemptRequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final QuestionsResponseMapper questionsResponseMapper;
    private final IdentityIQWebServiceSoap identityIQWebServiceSoap;
    private final MetricsService metricsService;

    KBVGateway(
            StartAuthnAttemptRequestMapper saaRequestMapper,
            ResponseToQuestionMapper responseToQuestionMapper,
            QuestionsResponseMapper questionsResponseMapper,
            IdentityIQWebServiceSoap identityIQWebServiceSoap,
            MetricsService metricsService) {
        this.identityIQWebServiceSoap =
                Objects.requireNonNull(
                        identityIQWebServiceSoap, "identityIQWebServiceSoap must not be null");
        this.saaRequestMapper =
                Objects.requireNonNull(saaRequestMapper, "saaRequestMapper must not be null");
        this.responseToQuestionMapper =
                Objects.requireNonNull(
                        responseToQuestionMapper, "rtqRequestMapper must not be null");
        this.questionsResponseMapper =
                Objects.requireNonNull(
                        questionsResponseMapper, "questionsResponseMapper must not be null");
        this.metricsService =
                Objects.requireNonNull(metricsService, "metricsService must not be null");
    }

    @Tracing
    public QuestionsResponse getQuestions(QuestionRequest questionRequest) {
        SAARequest saaRequest = saaRequestMapper.mapQuestionRequest(questionRequest);

        Instant start = Instant.now();
        long startTime = System.nanoTime();

        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "getQuestions",
                        "SAA",
                        "http://schema.uk.experian.com/Experian/IdentityIQ/Services/WebService/SAA");
        SAAResponse2 saaResponse2 = getQuestionRequestResponse(saaRequest);
        OpenTelemetryUtil.endSpan(span);

        long endTime = System.nanoTime();
        Instant end = Instant.now();
        long totalTimeInMs = (endTime - startTime) / 1000000;

        LOGGER.info("Get questions API response latency: latencyInMs={}", totalTimeInMs);

        metricsService
                .getEventProbe()
                .counterMetric("get_questions_duration", totalTimeInMs, Unit.MILLISECONDS);

        QuestionsResponse questionsResponse = questionsResponseMapper.mapSAAResponse(saaResponse2);

        if (questionsResponse.hasError()) {
            metricsService.sendErrorMetric(
                    questionsResponse.getErrorCode(), "initial_questions_response_error");
            logError(
                    "Question retrieval from the third party API resulted in an error",
                    questionsResponse);
        }

        sendResultMetric(
                EXPERIAN_INITIAL_QUESTION_RESPONSE,
                questionsResponse.getResults(),
                Duration.between(start, end).toMillis());

        logQuestionResponse(
                saaResponse2.getControl(), saaResponse2.getResults(), saaResponse2.getError());

        return questionsResponse;
    }

    @Tracing
    public QuestionsResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest) {
        RTQRequest rtqRequest =
                responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);

        Instant start = Instant.now();
        long startTime = System.nanoTime();

        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "submitAnswers",
                        "RTQ",
                        "http://schema.uk.experian.com/Experian/IdentityIQ/Services/WebService/RTQ");

        RTQResponse2 rtqResponse2 = submitQuestionAnswerResponse(rtqRequest);
        OpenTelemetryUtil.endSpan(span);

        long endTime = System.nanoTime();
        Instant end = Instant.now();

        long totalTimeInMs = (endTime - startTime) / 1000000;

        LOGGER.info("Submit answers API response latency: latencyInMs={}", totalTimeInMs);

        metricsService
                .getEventProbe()
                .counterMetric("submit_answers_duration", totalTimeInMs, Unit.MILLISECONDS);

        QuestionsResponse questionsResponse = questionsResponseMapper.mapRTQResponse(rtqResponse2);

        if (questionsResponse.hasError()) {
            this.metricsService.sendErrorMetric(
                    questionsResponse.getErrorCode(), "submit_questions_response_error");
            logError(
                    "Answer submission to the third party API resulted in an error",
                    questionsResponse);
        }

        sendResultMetric(
                EXPERIAN_SUBMIT_RESPONSE,
                questionsResponse.getResults(),
                Duration.between(start, end).toMillis());

        return questionsResponse;
    }

    @Tracing(segmentName = "getQuestionResponse")
    private SAAResponse2 getQuestionRequestResponse(SAARequest saaRequest) {
        TracingUtils.putAnnotation(EXPERIAN_IIQ_REQUEST, EXPERIAN_INITIAL_QUESTION_RESPONSE);
        return identityIQWebServiceSoap.saa(saaRequest);
    }

    @Tracing(segmentName = "submitQuestionAnswerResponse")
    private RTQResponse2 submitQuestionAnswerResponse(RTQRequest rtqRequest) {
        TracingUtils.putAnnotation(EXPERIAN_IIQ_REQUEST, EXPERIAN_SUBMIT_RESPONSE);
        return identityIQWebServiceSoap.rtq(rtqRequest);
    }

    private void logError(String context, QuestionsResponse questionsResponse) {
        LOGGER.error(
                "{} - Error code: {} and error message: {}",
                context,
                questionsResponse.getErrorCode(),
                questionsResponse.getErrorMessage());
    }

    private void sendResultMetric(String metricName, KbvResult result, long executionDuration) {
        if (Objects.nonNull(result)) {
            this.metricsService.sendResultMetric(result, metricName, executionDuration);
        }
    }

    private void logQuestionResponse(Control control, Results results, Error error) {
        if (LOGGER.isDebugEnabled()) {
            String urn = "";
            String authRefNo = "";
            String outcome = "";
            String authenticationResult = "";
            String transIds = "";
            String errorCode = "";
            String errorMessage = "";
            String confirmationCode = "";

            if (control != null) {
                urn = control.getURN();
                authRefNo = control.getAuthRefNo();
            }
            if (results != null) {
                outcome = results.getOutcome();
                authenticationResult = results.getAuthenticationResult();
                ArrayOfString nextTransId = results.getNextTransId();
                if (nextTransId != null) {
                    List<String> transId = nextTransId.getString();
                    if (transId != null) {
                        transIds = String.join(",", transId);
                    }
                }
                confirmationCode = results.getConfirmationCode();
            }

            if (error != null) {
                errorCode = error.getErrorCode();
                errorMessage = error.getMessage();
            }

            LOGGER.debug(
                    "question response: urn: {}, authRefNo: {}, outcome: {}, authenticationResult: {}, transIds: {}, error code: {}, error message: {}, confirmation code: {}",
                    urn,
                    authRefNo,
                    outcome,
                    authenticationResult,
                    transIds,
                    errorCode,
                    errorMessage,
                    confirmationCode);
        }
    }
}
