package uk.gov.di.ipv.cri.kbv.api.library.domain;

import java.util.List;

public class AnswerFormat {

    private String identifier;
    private String fieldType;
    private List<String> answerList;

    public String getIdentifier() {
        return identifier;
    }

    public String getFieldType() {
        return fieldType;
    }

    public List<String> getAnswerList() {
        return answerList;
    }
}
