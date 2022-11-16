package gov.uk.kbv.api.stepDefinitions;

import java.net.http.HttpResponse;

public class CriTestContext {
    private HttpResponse<String> response;

    private String sessionId;
    private String accessToken;
    private String serialisedUserIdentity;

    public HttpResponse<String> getResponse() {
        return response;
    }

    public void setResponse(HttpResponse<String> response) {
        this.response = response;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSerialisedUserIdentity() {
        return serialisedUserIdentity;
    }

    public void setSerialisedUserIdentity(String serialisedUserIdentity) {
        this.serialisedUserIdentity = serialisedUserIdentity;
    }
}
