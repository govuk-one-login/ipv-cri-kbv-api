package uk.gov.di.ipv.cri.kbv.api.domain;

import com.experian.uk.schema.experian.identityiq.services.webservice.Alerts;

public class KbvAlert {
    private String code;
    private String text;

    public KbvAlert() {}

    public KbvAlert(Alerts alert) {
        this.code = alert.getCode();
        this.text = alert.getText();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
