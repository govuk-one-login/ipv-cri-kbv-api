package uk.gov.di.ipv.cri.kbv.api.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.UUID;

@DynamoDbBean
public class KBVItem {
    private UUID sessionId;
    private String questionState;
    private long expiryDate;
    private String authRefNo;
    private String urn;
    private String status;

    public void setQuestionState(String questionState) {
        this.questionState = questionState;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestionState() {
        return questionState;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
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

    @DynamoDbPartitionKey
    public UUID getSessionId() {
        return sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
