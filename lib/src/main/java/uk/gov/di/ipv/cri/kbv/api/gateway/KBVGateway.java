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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.kbv.api.domain.KbvResult;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import java.util.List;
import java.util.Objects;

public class KBVGateway {
    private static final Logger LOGGER = LogManager.getLogger();

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
        SAAResponse2 saaResponse2 = identityIQWebServiceSoap.saa(saaRequest);
        QuestionsResponse questionsResponse = questionsResponseMapper.mapSAAResponse(saaResponse2);

        if (questionsResponse.hasError()) {
            metricsService.sendErrorMetric(
                    questionsResponse.getErrorCode(), "initial_questions_response_error");
            logError(
                    "Question retrieval from the third party API resulted in an error",
                    questionsResponse);
        }

        sendResultMetric("initial_questions_response", questionsResponse.getResults());

        logQuestionResponse(
                saaResponse2.getControl(), saaResponse2.getResults(), saaResponse2.getError());

        return questionsResponse;
    }

    @Tracing
    public QuestionsResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest) {
        RTQRequest rtqRequest =
                responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);
        RTQResponse2 rtqResponse2 = identityIQWebServiceSoap.rtq(rtqRequest);

        QuestionsResponse questionsResponse = questionsResponseMapper.mapRTQResponse(rtqResponse2);

        if (questionsResponse.hasError()) {
            this.metricsService.sendErrorMetric(
                    questionsResponse.getErrorCode(), "submit_questions_response_error");
            logError(
                    "Answer submission to the third party API resulted in an error",
                    questionsResponse);
        }
        sendResultMetric("submit_questions_response", questionsResponse.getResults());

        return questionsResponse;
    }

    private void logError(String context, QuestionsResponse questionsResponse) {
        LOGGER.error(
                "{} - Error code: {} and error message: {}",
                context,
                questionsResponse.getErrorCode(),
                questionsResponse.getErrorMessage());
    }

    private void sendResultMetric(String metricName, KbvResult result) {
        if (Objects.nonNull(result)) {
            this.metricsService.sendResultMetric(result, metricName);
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
