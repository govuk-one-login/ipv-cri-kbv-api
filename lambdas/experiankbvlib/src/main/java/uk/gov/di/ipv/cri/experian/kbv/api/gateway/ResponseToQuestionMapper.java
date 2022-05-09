package uk.gov.di.ipv.cri.experian.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Response;
import com.experian.uk.schema.experian.identityiq.services.webservice.Responses;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionsResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResponseToQuestionMapper {
    public RTQRequest mapQuestionAnswersRtqRequest(QuestionAnswerRequest questionAnswers) {
        Objects.requireNonNull(questionAnswers, "The QuestionAnswerRequest must not be null");

        var rtqRequest = new RTQRequest();
        var responses = new Responses();

        responses.getResponse().addAll(getResponses(questionAnswers.getQuestionAnswers()));

        rtqRequest.setControl(getControl(questionAnswers));
        rtqRequest.setResponses(responses);
        return rtqRequest;
    }

    public QuestionsResponse mapRTQResponse2ToMapQuestionsResponse(RTQResponse2 results) {
        QuestionsResponse questionAnswerResponse = new QuestionsResponse();

        questionAnswerResponse.setQuestions(results.getQuestions());
        questionAnswerResponse.setResults(results.getResults());
        questionAnswerResponse.setControl(results.getControl());
        questionAnswerResponse.setError(results.getError());

        return questionAnswerResponse;
    }

    private Control getControl(QuestionAnswerRequest questionAnswers) {
        var control = new Control();
        control.setAuthRefNo(questionAnswers.getAuthRefNo());
        control.setURN(questionAnswers.getUrn());
        return control;
    }

    private List<Response> getResponses(List<QuestionAnswer> questionAnswers) {
        return questionAnswers.stream()
                .map(
                        element -> {
                            Response response = new Response();
                            response.setQuestionID(element.getQuestionId());
                            response.setAnswerGiven(element.getAnswer());
                            response.setCustResponseFlag(0);
                            response.setAnswerActionFlag("U");
                            return response;
                        })
                .collect(Collectors.toList());
    }
}
