package uk.gov.di.ipv.cri.kbv.api.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SharedClaims {

    @JsonProperty("name")
    private List<Name> names;

    @JsonProperty("birthDate")
    private List<BirthDate> birthDate;

    @JsonProperty("@context")
    private List<String> context;

    @JsonProperty("addresses")
    private List<UKAddresses> ukAddresses;

    public List<Name> getNames() {
        return names;
    }

    public void setNames(List<Name> names) {
        this.names = names;
    }

    public List<BirthDate> getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(List<BirthDate> birthDate) {
        this.birthDate = birthDate;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public List<UKAddresses> getUkAddresses() {
        return ukAddresses;
    }

    public void setUkAddresses(List<UKAddresses> ukAddresses) {
        this.ukAddresses = ukAddresses;
    }

    @Override
    public String toString() {
        return "SharedClaims{"
                + "names="
                + names
                + ", birthDate="
                + birthDate
                + ", context="
                + context
                + ", ukAddresses="
                + ukAddresses
                + '}';
    }
}
