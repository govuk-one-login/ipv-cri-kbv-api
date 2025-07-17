package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;

public class KBVService {
    private final KBVGateway kbvGateway;

    public KBVService(ServiceFactory serviceFactory) {
        this.kbvGateway = serviceFactory.getKbvGateway();
    }

    public QuestionsResponse getQuestions(QuestionRequest questionRequest) {
        return this.kbvGateway.getQuestions(questionRequest);
    }

    public QuestionsResponse submitAnswers(QuestionAnswerRequest answers) {
        return kbvGateway.submitAnswers(answers);
    }
}
