package uk.gov.di.ipv.cri.kbv.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;
import org.jetbrains.annotations.NotNull;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedBuilder;

import java.time.LocalDate;
import java.util.List;

public interface PersonIdentityDetailState {
    @State("VC givenName is Mary")
    default void givenName() {}

    @State("VC familyName is Watson")
    default void familyName() {}

    @State("VC birthDate is 1932-02-25")
    default void birthDate() {}

    @State("VC address uprn is 10022812929")
    default void addressUprn() {}

    @State("VC address organisationName is FINCH GROUP")
    default void addressOrganisationName() {}

    @State("VC address subBuildingName is UNIT 2B")
    default void addressSubBuildingName() {}

    @State("VC address buildingNumber is 16")
    default void addressBuildingNumber() {}

    @State("VC address buildingName is COY POND BUSINESS PARK")
    default void addressBuildingName() {}

    @State("VC address dependentStreetName is KINGS PARK")
    default void addressDependentStreetName() {}

    @State("VC address streetName is BIG STREET")
    default void addressStreetName() {}

    @State("VC address doubleDependentAddressLocality is SOME DISTRICT")
    default void addressDoubleDependentLocality() {}

    @State("VC address dependentAddressLocality is LONG EATON")
    default void dependentAddressLocality() {}

    @State("VC address addressLocality is GREAT MISSENDEN")
    default void addressLocality() {}

    @State("VC address postalCode is HP16 0AL")
    default void postalCode() {}

    @State("VC address addressCountry is GB")
    default void addressCountry() {}

    default PersonIdentityDetailed createPersonIdentity() {
        return PersonIdentityDetailedBuilder.builder(
                        List.of(getName("Mary", "Watson")),
                        List.of(getBirthDate(LocalDate.of(1932, 2, 25))))
                .withAddresses(List.of(getAddress()))
                .build();
    }

    default Address getAddress() {
        Address address = new Address();
        address.setUprn(10022812929L);
        address.setOrganisationName("FINCH GROUP");
        address.setBuildingName("UNIT 2B");
        address.setBuildingNumber("16");
        address.setStreetName("Wellington Street");
        address.setDependentStreetName("KINGS PARK");
        address.setStreetName("BIG STREET");
        address.setDoubleDependentAddressLocality("SOME DISTRICT");
        address.setDependentAddressLocality("LONG EATON");
        address.setAddressLocality("GREAT MISSENDEN");
        address.setPostalCode("HP16 0AL");
        address.setAddressCountry("GB");
        return address;
    }

    @NotNull
    private BirthDate getBirthDate(LocalDate birthdate) {
        BirthDate birthDate = new BirthDate();
        birthDate.setValue(birthdate);
        return birthDate;
    }

    @NotNull
    private Name getName(String firstName, String givenName) {
        Name name = new Name();
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue(firstName);

        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue(givenName);

        name.setNameParts(List.of(firstNamePart, surnamePart));
        return name;
    }
}
