package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckDetail {
    @JsonProperty("checkMethod")
    private String checkMethod = "kbv";

    @JsonProperty("kbvResponseMode")
    private String kbvResponseMode = "multiple_choice";

    @JsonProperty("kbvQuality")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer kbvQuality;

    public Integer getKbvQuality() {
        return kbvQuality;
    }

    public void setKbvQuality(int kbvQuality) {
        this.kbvQuality = kbvQuality;
    }

    public String getCheckMethod() {
        return this.checkMethod;
    }

    public String getKbvResponseMode() {
        return this.kbvResponseMode;
    }
}
