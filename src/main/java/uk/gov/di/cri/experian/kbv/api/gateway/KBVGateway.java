package uk.gov.di.cri.experian.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionAnswerResponse;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;
import uk.gov.di.cri.experian.kbv.api.security.KbvSoapWebServiceClient;

import java.io.IOException;
import java.util.Objects;

public class KBVGateway {

    private final SAARequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final KbvSoapWebServiceClient kbvSoapWebServiceClient;

    public KBVGateway(
            SAARequestMapper saaRequestMapper,
            ResponseToQuestionMapper responseToQuestionMapper,
            KbvSoapWebServiceClient kbvSoapWebServiceClient) {
        Objects.requireNonNull(kbvSoapWebServiceClient, "httpClient must not be null");
        Objects.requireNonNull(saaRequestMapper, "saaRequestMapper must not be null");
        Objects.requireNonNull(responseToQuestionMapper, "rtqRequestMapper must not be null");
        this.saaRequestMapper = saaRequestMapper;
        this.responseToQuestionMapper = responseToQuestionMapper;
        this.kbvSoapWebServiceClient = kbvSoapWebServiceClient;
    }

    public QuestionsResponse getQuestions(PersonIdentity personIdentity)
            throws IOException, InterruptedException {
        SAARequest saaRequest = saaRequestMapper.mapPersonIdentity(personIdentity);
        IdentityIQWebServiceSoap identityIQWebServiceSoap =
                kbvSoapWebServiceClient.getIdentityIQWebServiceSoapEndpoint();
        SAAResponse2 saaResponse2 = identityIQWebServiceSoap.saa(saaRequest);
        return saaRequestMapper.mapSAAResponse2ToQuestionsResponse(saaResponse2);
    }

    public QuestionAnswerResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest)
            throws InterruptedException {
        RTQRequest rtqRequest =
                this.responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);

        IdentityIQWebServiceSoap identityIQWebServiceSoap =
                kbvSoapWebServiceClient.getIdentityIQWebServiceSoapEndpoint();
        RTQResponse2 rtqResponse2 = identityIQWebServiceSoap.rtq(rtqRequest);
        return this.responseToQuestionMapper.mapResultsToMapQuestionAnswersResponse(rtqResponse2);
    }
}
