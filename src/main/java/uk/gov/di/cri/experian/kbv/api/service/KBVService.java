package uk.gov.di.cri.experian.kbv.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswerResponse;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;
import uk.gov.di.cri.experian.kbv.api.gateway.KBVGateway;

public class KBVService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KBVService.class);
    private final KBVGateway kbvGateway;

    public KBVService(KBVGateway kbvGateway) {
        this.kbvGateway = kbvGateway;
    }

    public QuestionsResponse init(PersonIdentity personIdentity) {
        try {
            return this.kbvGateway.getQuestions(personIdentity);
        } catch (Exception e) {
            LOGGER.error("Error occurred when attempting to invoke experian api", e);
            return null;
        }
    }

    public QuestionAnswerResponse submitAnswers(QuestionAnswerRequest answers) {
        try {
            return kbvGateway.submitAnswers(answers);
        } catch (InterruptedException ie) {
            LOGGER.error("Error occurred when attempting to invoke experian api", ie);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            LOGGER.error("Error occurred when attempting to invoke experian api", e);
            return null;
        }
    }
}
