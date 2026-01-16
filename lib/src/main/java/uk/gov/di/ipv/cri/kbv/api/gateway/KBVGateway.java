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
import jakarta.xml.ws.WebServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.exception.TimeoutException;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;
import uk.gov.di.ipv.cri.kbv.api.util.OpenTelemetryUtil;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Objects;

public class KBVGateway {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String EXPERIAN_INITIAL_QUESTION_RESPONSE = "initial_questions_response";
    private static final String EXPERIAN_INITIAL_QUESTION_DURATION = "get_questions_duration";
    private static final String EXPERIAN_INITIAL_QUESTION_ERROR =
            "initial_questions_response_error";
    private static final String EXPERIAN_SUBMIT_RESPONSE = "submit_questions_response";
    private static final String EXPERIAN_SUBMIT_DURATION = "submit_answers_duration";
    private static final String EXPERIAN_SUBMIT_ERROR = "submit_questions_response_error";
    private static final String EXPERIAN_INITIAL_QUESTION_TIMEOUT =
            "initial_questions_response_timeout";
    private static final String EXPERIAN_SUBMIT_RESPONSE_TIMEOUT =
            "submit_questions_response_timeout";

    private final StartAuthnAttemptRequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final QuestionsResponseMapper questionsResponseMapper;
    private final MetricsService metricsService;

    KBVGateway(
            StartAuthnAttemptRequestMapper saaRequestMapper,
            ResponseToQuestionMapper responseToQuestionMapper,
            QuestionsResponseMapper questionsResponseMapper,
            MetricsService metricsService) {
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

    public QuestionsResponse getQuestions(
            IdentityIQWebServiceSoap identityIQWebServiceSoap, QuestionRequest questionRequest) {
        SAARequest saaRequest = saaRequestMapper.mapQuestionRequest(questionRequest);

        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "getQuestions",
                        "SAA",
                        "http://schema.uk.experian.com/Experian/IdentityIQ/Services/WebService/SAA");

        long startTime = System.nanoTime();

        SAAResponse2 saaResponse2;
        try {
            saaResponse2 = getQuestionRequestResponse(identityIQWebServiceSoap, saaRequest);
        } catch (TimeoutException te) {
            LOGGER.error("Question retrieval to the third party API timed out", te);
            metricsService.sendErrorMetric(EXPERIAN_INITIAL_QUESTION_TIMEOUT, "TIMEOUT");
            return null;
        } finally {
            long totalTimeInMs = (System.nanoTime() - startTime) / 1000000;

            OpenTelemetryUtil.endSpan(span);

            LOGGER.info("Get questions API response latency: latencyInMs={}", totalTimeInMs);
            metricsService.sendDurationMetric(EXPERIAN_INITIAL_QUESTION_DURATION, totalTimeInMs);
        }

        QuestionsResponse questionsResponse = questionsResponseMapper.mapSAAResponse(saaResponse2);

        if (questionsResponse.hasError()) {
            metricsService.sendErrorMetric(
                    EXPERIAN_INITIAL_QUESTION_ERROR, questionsResponse.getErrorCode());
            logError(
                    "Question retrieval from the third party API resulted in an error",
                    questionsResponse);
        }

        metricsService.sendResultMetric(
                EXPERIAN_INITIAL_QUESTION_RESPONSE, questionsResponse.getResults());

        logQuestionResponse(
                saaResponse2.getControl(), saaResponse2.getResults(), saaResponse2.getError());

        return questionsResponse;
    }

    public QuestionsResponse submitAnswers(
            IdentityIQWebServiceSoap identityIQWebServiceSoap,
            QuestionAnswerRequest questionAnswerRequest) {
        RTQRequest rtqRequest =
                responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);

        long startTime = System.nanoTime();

        Span span =
                OpenTelemetryUtil.createSpan(
                        this.getClass(),
                        "submitAnswers",
                        "RTQ",
                        "http://schema.uk.experian.com/Experian/IdentityIQ/Services/WebService/RTQ");

        RTQResponse2 rtqResponse2;
        try {
            rtqResponse2 = submitQuestionAnswerResponse(identityIQWebServiceSoap, rtqRequest);
        } catch (TimeoutException te) {
            LOGGER.error("Answer submission to the third party API timed out", te);
            metricsService.sendErrorMetric(EXPERIAN_SUBMIT_RESPONSE_TIMEOUT, "TIMEOUT");
            return null;
        } finally {
            long totalTimeInMs = (System.nanoTime() - startTime) / 1000000;

            OpenTelemetryUtil.endSpan(span);

            LOGGER.info("Submit answers API response latency: latencyInMs={}", totalTimeInMs);
            metricsService.sendDurationMetric(EXPERIAN_SUBMIT_DURATION, totalTimeInMs);
        }

        QuestionsResponse questionsResponse = questionsResponseMapper.mapRTQResponse(rtqResponse2);

        if (questionsResponse.hasError()) {
            this.metricsService.sendErrorMetric(
                    EXPERIAN_SUBMIT_ERROR, questionsResponse.getErrorCode());
            logError(
                    "Answer submission to the third party API resulted in an error",
                    questionsResponse);
        }

        metricsService.sendResultMetric(EXPERIAN_SUBMIT_RESPONSE, questionsResponse.getResults());

        return questionsResponse;
    }

    protected SAAResponse2 getQuestionRequestResponse(
            IdentityIQWebServiceSoap identityIQWebServiceSoap, SAARequest saaRequest) {
        try {
            return identityIQWebServiceSoap.saa(saaRequest);
        } catch (WebServiceException wse) {
            if (wse.getCause() instanceof SocketTimeoutException
                    || wse.getCause() instanceof HttpTimeoutException) {
                throw new TimeoutException("SAA response timed out ");
            }
            throw wse;
        }
    }

    protected RTQResponse2 submitQuestionAnswerResponse(
            IdentityIQWebServiceSoap identityIQWebServiceSoap, RTQRequest rtqRequest) {
        try {
            return identityIQWebServiceSoap.rtq(rtqRequest);
        } catch (WebServiceException wse) {
            if (wse.getCause() instanceof SocketTimeoutException
                    || wse.getCause() instanceof HttpTimeoutException) {
                throw new TimeoutException("RTQ response timed out ");
            }
            throw wse;
        }
    }

    private void logError(String context, QuestionsResponse questionsResponse) {
        LOGGER.error(
                "{} - Error code: {} and error message: {}",
                context,
                questionsResponse.getErrorCode(),
                questionsResponse.getErrorMessage());
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
