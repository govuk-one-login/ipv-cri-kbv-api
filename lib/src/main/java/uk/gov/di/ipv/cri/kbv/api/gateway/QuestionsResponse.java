package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import uk.gov.di.ipv.cri.kbv.api.util.StringUtils;

import java.util.stream.Collectors;

public class QuestionsResponse {

    protected Control control;
    protected Questions questions;
    protected Results results;
    protected Error error;

    public Control getControl() {
        return control;
    }

    public void setControl(Control control) {
        this.control = control;
    }

    public Questions getQuestions() {
        return questions;
    }

    public void setQuestions(Questions questions) {
        this.questions = questions;
    }

    public boolean hasQuestions() {
        return this.getQuestions() != null;
    }

    public Results getResults() {
        return results;
    }

    public boolean hasQuestionRequestEnded() {
        if (StringUtils.isNotBlank(this.getQuestionStatus())) {
            return this.getQuestionStatus().equalsIgnoreCase("END");
        }
        return false;
    }

    public String getQuestionStatus() {
        return results.getNextTransId().getString().stream().collect(Collectors.joining(""));
    }

    public String getStatus() {
        return results.getAuthenticationResult();
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
