package uk.gov.di.ipv.cri.experian.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionsResponse;

import java.util.Objects;

public class KBVGateway {

    private final StartAuthnAttemptRequestMapper saaRequestMapper;
    private final ResponseToQuestionMapper responseToQuestionMapper;
    private final IdentityIQWebServiceSoap identityIQWebServiceSoap;

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
        SAARequest saaRequest = saaRequestMapper.mapQuestionRequest(questionRequest);
        SAAResponse2 saaResponse2 = identityIQWebServiceSoap.saa(saaRequest);
        return saaRequestMapper.mapSAAResponse2ToQuestionsResponse(saaResponse2);
    }

    public QuestionsResponse submitAnswers(QuestionAnswerRequest questionAnswerRequest)
            throws InterruptedException {
        RTQRequest rtqRequest =
                this.responseToQuestionMapper.mapQuestionAnswersRtqRequest(questionAnswerRequest);

        RTQResponse2 rtqResponse2 = identityIQWebServiceSoap.rtq(rtqRequest);
        return this.responseToQuestionMapper.mapRTQResponse2ToMapQuestionsResponse(rtqResponse2);
    }
}
