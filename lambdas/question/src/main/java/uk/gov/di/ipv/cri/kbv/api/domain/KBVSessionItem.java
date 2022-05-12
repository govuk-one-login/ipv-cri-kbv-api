package uk.gov.di.ipv.cri.kbv.api.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class KBVSessionItem {
    private String sessionId;
    private String authorizationCode;
    private String token;
    private String questionState;
    private String expiryDate;
    private String authRefNo;
    private String urn;
    private String userAttributes;
    private String status;

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

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAuthRefNo() {
        return authRefNo;
    }

    public void setAuthRefNo(String authRefNo) {
        this.authRefNo = authRefNo;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(String userAttributes) {
        this.userAttributes = userAttributes;
    }

    @DynamoDbPartitionKey
    public String getSessionId() {
        return sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
