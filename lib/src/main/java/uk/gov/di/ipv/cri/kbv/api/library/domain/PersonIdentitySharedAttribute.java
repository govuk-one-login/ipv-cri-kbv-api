package uk.gov.di.ipv.cri.kbv.api.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PersonIdentitySharedAttribute {

    private List<Names> names;

    private List<String> datesOfBirth;

    @JsonProperty("UKAddresses")
    private List<UKAddresses> ukAddresses;

    public List<Names> getNames() {
        return names;
    }

    public void setNames(List<Names> names) {
        this.names = names;
    }

    public List<String> getDatesOfBirth() {
        return datesOfBirth;
    }

    public void setDatesOfBirth(List<String> datesOfBirth) {
        this.datesOfBirth = datesOfBirth;
    }

    public List<UKAddresses> getUkAddresses() {
        return ukAddresses;
    }

    public void setUkAddresses(List<UKAddresses> ukAddresses) {
        this.ukAddresses = ukAddresses;
    }
}
