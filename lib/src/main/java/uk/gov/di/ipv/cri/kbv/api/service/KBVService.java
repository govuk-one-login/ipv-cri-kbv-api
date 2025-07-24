package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;

public class KBVService {
    private final KBVGateway kbvGateway;

    public KBVService(KBVGateway kbvGateway) {
        this.kbvGateway = kbvGateway;
    }

    public QuestionsResponse getQuestions(
            IdentityIQWebServiceSoap identityIQWebServiceSoap, QuestionRequest questionRequest) {
        return this.kbvGateway.getQuestions(identityIQWebServiceSoap, questionRequest);
    }

    public QuestionsResponse submitAnswers(
            IdentityIQWebServiceSoap identityIQWebServiceSoap, QuestionAnswerRequest answers) {
        return kbvGateway.submitAnswers(identityIQWebServiceSoap, answers);
    }
}
