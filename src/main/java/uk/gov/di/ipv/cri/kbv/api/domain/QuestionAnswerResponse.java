package uk.gov.di.ipv.cri.kbv.api.domain;

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
