package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionsResponse;
import uk.gov.di.ipv.cri.kbv.api.persistence.item.KBVSessionItem;

import java.util.UUID;

public class QuestionStore {
    private KBVSessionItem kbvSessionItem;
    private QuestionState questionState;
    private StorageService storageService;
    private static final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    public QuestionStore(
            KBVSessionItem kbvSessionItem,
            QuestionState questionState,
            StorageService storageService) {
        this.kbvSessionItem = kbvSessionItem;
        this.questionState = questionState;
        this.storageService = storageService;
    }

    public void saveResponsesToState(QuestionsResponse questionsResponse) {
        this.setQuestionState(questionsResponse);
        System.out.println("About to update savaResponsesToState");

        this.storageService.update(kbvSessionItem);
        System.out.println(kbvSessionItem);
    }

    public void setControlInfo(QuestionsResponse questionsResponse) {
        this.kbvSessionItem.setAuthRefNo(questionsResponse.getControl().getAuthRefNo());
        this.kbvSessionItem.setUrn(questionsResponse.getControl().getURN());
    }

    public void saveFinalResponseState(QuestionsResponse questionsResponse) {
        this.kbvSessionItem.setAuthorizationCode(UUID.randomUUID().toString());
        this.kbvSessionItem.setStatus(questionsResponse.getStatus());

        setQuestionState(questionsResponse);
        System.out.println("About to update saveFinalResponseState");
        storageService.update(kbvSessionItem);
        System.out.println(kbvSessionItem);
    }

    private void setQuestionState(QuestionsResponse questionsResponse) {
        try {
            this.questionState.setQAPairs(questionsResponse.getQuestions());
            this.questionState.setState(questionsResponse.getQuestionStatus());
            this.kbvSessionItem.setQuestionState(objectMapper.writeValueAsString(questionState));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
