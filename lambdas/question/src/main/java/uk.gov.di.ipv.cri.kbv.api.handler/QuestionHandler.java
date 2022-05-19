package uk.gov.di.ipv.cri.kbv.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.parameters.ParamManager;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.gateway.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.service.KBVService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVServiceFactory;
import uk.gov.di.ipv.cri.kbv.api.service.KBVStorageService;
import uk.gov.di.ipv.cri.kbv.api.service.KBVSystemProperty;
import uk.gov.di.ipv.cri.kbv.api.service.KeyStoreService;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

public class QuestionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String HEADER_SESSION_ID = "session-id";
    public static final String GET_QUESTION = "get_question";
    public static final String ERROR_KEY = "\"error\"";
    private static ObjectMapper objectMapper;
    private final KBVStorageService kbvStorageService;
    private APIGatewayProxyResponseEvent response;
    private EventProbe eventProbe;

    private KBVService kbvService;

    @ExcludeFromGeneratedCoverageReport
    public QuestionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService =
                new KBVStorageService(
                        new DataStore<KBVItem>(
                                getKBVTableName(), KBVItem.class, DataStore.getClient()));
        this.kbvService = new KBVServiceFactory().create();
        var kbvSystemProperty =
                new KBVSystemProperty(new KeyStoreService(ParamManager.getSecretsProvider()));

        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = new EventProbe();

        kbvSystemProperty.save();
    }

    public QuestionHandler(
            ObjectMapper objectMapper,
            KBVStorageService kbvStorageService,
            KBVSystemProperty systemProperty,
            KBVServiceFactory kbvServiceFactory,
            EventProbe eventProbe) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.kbvStorageService = kbvStorageService;
        this.kbvService = kbvServiceFactory.create();

        this.response = new APIGatewayProxyResponseEvent();
        this.eventProbe = eventProbe;

        systemProperty.save();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            processQuestionRequest(input);
        } catch (JsonProcessingException jsonProcessingException) {
            eventProbe.log(ERROR, jsonProcessingException).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody(
                    "{ " + ERROR_KEY + ":\"Failed to parse object using ObjectMapper.\" }");
        } catch (NullPointerException npe) {
            eventProbe.log(INFO, npe).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody("{ " + ERROR_KEY + ":\"" + npe + "\" }");
        } catch (IOException | InterruptedException e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"Retrieving questions failed.\" }");
        } catch (Exception e) {
            eventProbe.log(ERROR, e).counterMetric(GET_QUESTION, 0d);
            response.withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.withBody("{ " + ERROR_KEY + ":\"AWS Server error occurred.\" }");
        }
        return response;
    }

    public void processQuestionRequest(APIGatewayProxyRequestEvent input)
            throws IOException, InterruptedException {
        response.withHeaders(Map.of("Content-Type", "application/json"));
        String sessionId = input.getHeaders().get(HEADER_SESSION_ID);
        System.out.println("Session ID ====> " + sessionId);
        KBVItem kbvItem =
                kbvStorageService.getSessionId(sessionId).orElseThrow(NullPointerException::new);

        //        PersonIdentity personIdentity =
        //                objectMapper.readValue(kbvSessionItem.getUserAttributes(),
        // PersonIdentity.class);
        //        QuestionState questionState =
        //                objectMapper.readValue(kbvSessionItem.getQuestionState(),
        // QuestionState.class);

        QuestionRequest questionRequest = new QuestionRequest();
        //        questionRequest.setPersonIdentity(personIdentity);
        //
        //        if (respondWithQuestionFromDbStore(questionState)) return;
        //        respondWithQuestionFromExperianThenStoreInDb(
        //                questionRequest, kbvSessionItem, questionState);
    }

    private boolean respondWithQuestionFromDbStore(QuestionState questionState)
            throws JsonProcessingException {
        // TODO Handle scenario when no questions are available
        Optional<Question> nextQuestion = questionState.getNextQuestion();
        if (nextQuestion.isPresent()) {
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));
            response.withStatusCode(HttpStatus.SC_OK);
            return true;
        }
        return false;
    }

    private void respondWithQuestionFromExperianThenStoreInDb(
            QuestionRequest questionRequest, KBVItem kbvItem, QuestionState questionState)
            throws IOException, InterruptedException {
        // we should fall in this block once only
        // fetch a batch of questions from experian kbv wrapper
        if (kbvItem.getAuthorizationCode() != null) {
            response.withStatusCode(HttpStatus.SC_NO_CONTENT);
            return;
        }
        QuestionsResponse questionsResponse = this.kbvService.getQuestions(questionRequest);
        if (questionsResponse.hasQuestions()) {
            questionState.setQAPairs(questionsResponse.getQuestions());
            Optional<Question> nextQuestion = questionState.getNextQuestion();
            response.withStatusCode(HttpStatus.SC_OK);
            response.withBody(objectMapper.writeValueAsString(nextQuestion.get()));

            String state = objectMapper.writeValueAsString(questionState);
            //            kbvSessionItem.setQuestionState(state);
            //
            // kbvSessionItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
            //            kbvSessionItem.setUrn(questionsResponse.getControl().getURN());
            kbvStorageService.update(kbvItem);
        } else { // TODO: Alternate flow when first request does not return questions
            response.withStatusCode(HttpStatus.SC_BAD_REQUEST);
            response.withBody(objectMapper.writeValueAsString(questionsResponse));
        }
    }

    public String getKBVTableName() {
        return ParamManager.getSsmProvider().get(getParameterName("KBVTableName"));
    }

    public String getParameterName(String parameterName) {
        String parameterPrefix =
                Objects.requireNonNull(
                        System.getenv("AWS_STACK_NAME"), "env var AWS_STACK_NAME required");
        return String.format("/%s/%s", parameterPrefix, parameterName);
    }
}
