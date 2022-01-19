package uk.gov.di.cri.experian.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;

public class QuestionAnswerResponse {
    protected Control control;
    protected Questions questions;
    protected Results results;
    protected Error error;

    public Control getControl() {
        return control;
    }

    public void setControl(Control value) {
        this.control = value;
    }

    public Questions getQuestions() {
        return questions;
    }

    public void setQuestions(Questions value) {
        this.questions = value;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results value) {
        this.results = value;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error value) {
        this.error = value;
    }
}
