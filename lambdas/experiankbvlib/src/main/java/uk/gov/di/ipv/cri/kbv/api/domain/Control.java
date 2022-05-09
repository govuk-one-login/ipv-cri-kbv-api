package uk.gov.di.ipv.cri.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Parameters;

public class Control {
    private String authRefNo;
    private String urn;
    private String dateTime;
    private String testDatabase;
    private String clientAccountNo;
    private String clientBranchNo;
    private String operatorID;
    private Parameters parameters;

    public String getAuthRefNo() {
        return authRefNo;
    }

    public String getURN() {
        return urn;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getTestDatabase() {
        return testDatabase;
    }

    public String getClientAccountNo() {
        return clientAccountNo;
    }

    public String getClientBranchNo() {
        return clientBranchNo;
    }

    public String getOperatorID() {
        return operatorID;
    }

    public Parameters getParameters() {
        return parameters;
    }
}
