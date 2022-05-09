package uk.gov.di.ipv.cri.experian.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;

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

    public Results getResults() {
        return results;
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
