package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequest;

class StartAuthnAttemptRequestMapperTest {
    private StartAuthnAttemptRequestMapper startAuthnAttemptRequestMapper;
    QuestionRequest questionRequest;

    @BeforeEach
    void setUp() {
        startAuthnAttemptRequestMapper = new StartAuthnAttemptRequestMapper("Static");
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForCurrentAddress() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();
        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);
        Address personAddress = personIdentity.getAddresses().get(0);
        assertAll(
                //                () ->
                //                        assertEquals(
                //                                result.getApplicant().getName().getTitle(),
                //                                personIdentity.getTitle()),
                () ->
                        assertEquals(
                                result.getApplicant().getName().getForename(),
                                personIdentity.getFirstName()),
                () ->
                        assertEquals(
                                result.getApplicant().getName().getSurname(),
                                personIdentity.getSurname()),
                () ->
                        assertEquals(
                                result.getApplicant().getDateOfBirth().getCCYY(),
                                personIdentity.getDateOfBirth().getYear()),
                () ->
                        assertEquals(
                                result.getApplicant().getDateOfBirth().getMM(),
                                personIdentity.getDateOfBirth().getMonth().getValue()),
                () ->
                        assertEquals(
                                result.getApplicant().getDateOfBirth().getDD(),
                                personIdentity.getDateOfBirth().getDayOfMonth()),
                () ->
                        assertEquals(
                                personAddress.getBuildingName(),
                                result.getLocationDetails().get(0).getUKLocation().getHouseName()),
                () ->
                        assertEquals(
                                personAddress.getPostalCode(),
                                result.getLocationDetails().get(0).getUKLocation().getPostcode()),
                () ->
                        assertEquals(
                                personAddress.getAddressLocality(),
                                result.getLocationDetails().get(0).getUKLocation().getPostTown()),
                () ->
                        assertEquals(
                                personAddress.getStreetName(),
                                result.getLocationDetails().get(0).getUKLocation().getStreet()));
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForPreviousAddress() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();

        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);
        Address personAddress = personIdentity.getAddresses().get(0);
        assertAll(
                () -> assertEquals("Static", result.getControl().getTestDatabase()),
                () -> assertEquals("urn", result.getControl().getURN()),
                () -> assertEquals("1 out of 2", result.getApplicationData().getProduct()),
                //                () ->
                //                        assertEquals(
                //                                personIdentity.getTitle(),
                //                                result.getApplicant().getName().getTitle()),
                () ->
                        assertEquals(
                                personIdentity.getFirstName(),
                                result.getApplicant().getName().getForename()),
                () ->
                        assertEquals(
                                personIdentity.getSurname(),
                                result.getApplicant().getName().getSurname()),
                () ->
                        assertEquals(
                                personIdentity.getDateOfBirth().getYear(),
                                result.getApplicant().getDateOfBirth().getCCYY()),
                () ->
                        assertEquals(
                                personIdentity.getDateOfBirth().getMonth().getValue(),
                                result.getApplicant().getDateOfBirth().getMM()),
                () ->
                        assertEquals(
                                personIdentity.getDateOfBirth().getDayOfMonth(),
                                result.getApplicant().getDateOfBirth().getDD()),
                () ->
                        assertEquals(
                                personAddress.getBuildingName(),
                                result.getLocationDetails().get(0).getUKLocation().getHouseName()),
                () ->
                        assertEquals(
                                personAddress.getPostalCode(),
                                result.getLocationDetails().get(0).getUKLocation().getPostcode()),
                () ->
                        assertEquals(
                                personAddress.getAddressLocality(),
                                result.getLocationDetails().get(0).getUKLocation().getPostTown()),
                () ->
                        assertEquals(
                                personAddress.getStreetName(),
                                result.getLocationDetails().get(0).getUKLocation().getStreet()));
    }

    @Test
    void shouldThrowExceptionWhenQuestionRequestIsNull() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest));
        assertEquals("The QuestionRequest must not be null", exception.getMessage());
    }
}
