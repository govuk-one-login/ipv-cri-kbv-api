package uk.gov.di.ipv.cri.kbv.api.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class KBVSessionItem {
    private String sessionId;
    private String authorizationCode;
    private String token;
    private String questionState;

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setQuestionState(String questionState) {
        this.questionState = questionState;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getToken() {
        return token;
    }

    public String getQuestionState() {
        return questionState;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    @DynamoDbPartitionKey
    public String getSessionId() {
        return sessionId;
    }
}
