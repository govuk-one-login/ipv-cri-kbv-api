package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;

import java.util.Objects;

public class KBVGateway {

    private static final Logger LOGGER = LogManager.getLogger();
    private final StartAuthnAttemptRequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final IdentityIQWebServiceSoap identityIQWebServiceSoap;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public KBVGateway(
            StartAuthnAttemptRequestMapper saaRequestMapper,
            ResponseToQuestionMapper responseToQuestionMapper,
            IdentityIQWebServiceSoap identityIQWebServiceSoap) {
        Objects.requireNonNull(identityIQWebServiceSoap, "httpClient must not be null");
        Objects.requireNonNull(saaRequestMapper, "saaRequestMapper must not be null");
        Objects.requireNonNull(responseToQuestionMapper, "rtqRequestMapper must not be null");
        this.saaRequestMapper = saaRequestMapper;
        this.responseToQuestionMapper = responseToQuestionMapper;
        this.identityIQWebServiceSoap = identityIQWebServiceSoap;
    }

    public QuestionsResponse getQuestions(QuestionRequest questionRequest) {
        try {
            LOGGER.info(
                    "Experian:getQuestions DI request: "
                            + mapper.writeValueAsString(questionRequest));
            SAARequest saaRequest = saaRequestMapper.mapQuestionRequest(questionRequest);
            LOGGER.info(
                    "Experian:getQuestions mapped request: "
                            + mapper.writeValueAsString(saaRequest));
            SAAResponse2 saaResponse2 = identityIQWebServiceSoap.saa(saaRequest);
            LOGGER.info(
                    "Experian:getQuestions raw response: "
                            + mapper.writeValueAsString(saaResponse2));
            var questionResponse =
                    saaRequestMapper.mapSAAResponse2ToQuestionsResponse(saaResponse2);
            LOGGER.info(
                    "Experian:getQuestions Mapped questionResponse: "
                            + mapper.writeValueAsString(questionResponse));
            return questionResponse;
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Error serializing Experian:getQuestions request/response: " + e.getMessage(),
                    e);
            return null;
        }
    }

    public QuestionsResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest) {
        try {
            LOGGER.info(
                    "Experian:submitAnswers DI request: "
                            + mapper.writeValueAsString(questionAnswerRequest));
            RTQRequest rtqRequest =
                    this.responseToQuestionMapper.mapQuestionAnswersRtqRequest(
                            questionAnswerRequest);
            LOGGER.info(
                    "Experian:submitAnswers mapped request: "
                            + mapper.writeValueAsString(rtqRequest));
            RTQResponse2 rtqResponse2 = identityIQWebServiceSoap.rtq(rtqRequest);
            LOGGER.info(
                    "Experian:submitAnswers raw response: "
                            + mapper.writeValueAsString(rtqResponse2));
            var questionResponse =
                    this.responseToQuestionMapper.mapRTQResponse2ToMapQuestionsResponse(
                            rtqResponse2);
            LOGGER.info(
                    "Experian:submitAnswers Mapped questionResponse: "
                            + mapper.writeValueAsString(questionResponse));
            return questionResponse;
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Error serializing Experian:submitAnswers request/response: " + e.getMessage(),
                    e);
            return null;
        }
    }
}
