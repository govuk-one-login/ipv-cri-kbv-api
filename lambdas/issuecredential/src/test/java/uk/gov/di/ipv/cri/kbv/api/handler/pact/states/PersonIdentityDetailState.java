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
    Address address = new Address();
    BirthDate birthDate = new BirthDate();
    NamePart firstNamePart = new NamePart();
    NamePart surnamePart = new NamePart();

    @State("VC givenName is Mary")
    default void givenName() {
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Mary");
    }

    @State("VC familyName is Watson")
    default void familyName() {
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Watson");
    }

    @State("VC birthDate is 1932-02-25")
    default void birthDate() {
        birthDate.setValue(LocalDate.of(1932, 2, 25));
    }

    @State("VC address uprn is 10022812929")
    default void addressUprn() {
        address.setUprn(10022812929L);
    }

    @State("VC address organisationName is FINCH GROUP")
    default void addressOrganisationName() {
        address.setOrganisationName("FINCH GROUP");
    }

    @State("VC address subBuildingName is UNIT 2B")
    default void addressSubBuildingName() {
        address.setSubBuildingName("UNIT 2B");
    }

    @State("VC address buildingNumber is 16")
    default void addressBuildingNumber() {
        address.setBuildingNumber("16");
    }

    @State("VC address buildingName is COY POND BUSINESS PARK")
    default void addressBuildingName() {
        address.setBuildingName("COY POND BUSINESS PARK");
    }

    @State("VC address dependentStreetName is KINGS PARK")
    default void addressDependentStreetName() {
        address.setDependentStreetName("KINGS PARK");
    }

    @State("VC address streetName is BIG STREET")
    default void addressStreetName() {
        address.setStreetName("BIG STREET");
    }

    @State("VC address doubleDependentAddressLocality is SOME DISTRICT")
    default void addressDoubleDependentLocality() {
        address.setDoubleDependentAddressLocality("SOME DISTRICT");
    }

    @State("VC address dependentAddressLocality is LONG EATON")
    default void dependentAddressLocality() {
        address.setDependentAddressLocality("LONG EATON");
    }

    @State("VC address addressLocality is GREAT MISSENDEN")
    default void addressLocality() {
        address.setAddressLocality("GREAT MISSENDEN");
    }

    @State("VC address postalCode is HP16 0AL")
    default void postalCode() {
        address.setPostalCode("HP16 0AL");
    }

    @State("VC address addressCountry is GB")
    default void addressCountry() {
        address.setAddressCountry("GB");
    }

    default PersonIdentityDetailed createPersonIdentity() {
        return PersonIdentityDetailedBuilder.builder(List.of(getName()), List.of(getBirthDate()))
                .withAddresses(List.of(getAddress()))
                .build();
    }

    @NotNull
    private Name getName() {
        Name name = new Name();
        name.setNameParts(List.of(firstNamePart(), surnamePart()));
        return name;
    }

    private Address getAddress() {
        return address;
    }

    private BirthDate getBirthDate() {
        return birthDate;
    }

    private NamePart firstNamePart() {
        return firstNamePart;
    }

    private NamePart surnamePart() {
        return surnamePart;
    }
}
