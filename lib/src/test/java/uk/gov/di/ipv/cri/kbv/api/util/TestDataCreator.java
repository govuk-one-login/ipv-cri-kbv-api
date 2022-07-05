package uk.gov.di.ipv.cri.kbv.api.util;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswer;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionAnswerRequest;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TestDataCreator {
    public static PersonIdentity createTestPersonIdentity(AddressType addressType) {
        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setDateOfBirth(LocalDate.of(1976, 12, 26));
        Address address = new Address();
        address.setPostalCode("Postcode");
        address.setStreetName("Street name");
        address.setAddressLocality("PostTown");
        address.setValidFrom(LocalDate.now().minus(2, ChronoUnit.YEARS));
        personIdentity.setAddresses(List.of(address));
        return personIdentity;
    }

    public static PersonIdentity createTestPersonIdentityWithDuplicateAddresses(
            AddressType addressType) {
        PersonIdentity personIdentity = new PersonIdentity();
        personIdentity.setDateOfBirth(LocalDate.of(1976, 12, 26));

        Address address1 = new Address();
        address1.setPostalCode("Postcode 1");
        address1.setStreetName("Street name 1");
        address1.setAddressLocality("PostTown");
        address1.setValidFrom(LocalDate.now().minus(2, ChronoUnit.YEARS));

        Address address2 = new Address();
        address2.setPostalCode("Postcode 2");
        address2.setStreetName("Street name 2");
        address2.setAddressLocality("PostTown");
        address2.setValidFrom(LocalDate.now().minus(1, ChronoUnit.YEARS));

        Address address3 = new Address();
        address3.setPostalCode("Postcode 1");
        address3.setStreetName("Street name 1");
        address3.setAddressLocality("PostTown");

        Address address4 = new Address();
        address4.setPostalCode("Postcode 2");
        address4.setStreetName("Street name 2");
        address4.setAddressLocality("PostTown");

        personIdentity.setAddresses(List.of(address1, address2, address3, address4));
        return personIdentity;
    }

    public static PersonIdentity createTestPersonIdentity() {
        return createTestPersonIdentity(AddressType.CURRENT);
    }

    public static QuestionAnswerRequest createTestQuestionAnswerRequest() {
        QuestionAnswerRequest answerRequest = new QuestionAnswerRequest();
        answerRequest.setUrn("urn");
        answerRequest.setAuthRefNo("auth-ref-no");
        answerRequest.setQuestionAnswers(List.of(new QuestionAnswer(), new QuestionAnswer()));
        return answerRequest;
    }

    public static QuestionRequest createTestQuestionAnswerRequest(AddressType addressType) {
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setUrn("urn");
        questionRequest.setStrategy("1 out of 2");
        questionRequest.setPersonIdentity(createTestPersonIdentity(addressType));
        return questionRequest;
    }

    public static QuestionRequest createTestQuestionAnswerRequestWithDuplicateAddresses(
            AddressType addressType) {
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setUrn("urn");
        questionRequest.setStrategy("1 out of 2");
        questionRequest.setPersonIdentity(
                createTestPersonIdentityWithDuplicateAddresses(addressType));
        return questionRequest;
    }
}
