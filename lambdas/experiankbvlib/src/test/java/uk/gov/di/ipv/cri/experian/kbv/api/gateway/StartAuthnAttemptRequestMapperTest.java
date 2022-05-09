package uk.gov.di.ipv.cri.experian.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.AddressType;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.QuestionRequest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.di.ipv.cri.experian.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequest;

class StartAuthnAttemptRequestMapperTest {
    private StartAuthnAttemptRequestMapper startAuthnAttemptRequestMapper;
    QuestionRequest questionRequest;

    @BeforeEach
    void setUp() {
        startAuthnAttemptRequestMapper = new StartAuthnAttemptRequestMapper();
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForCurrentAddress() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();
        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);

        assertAll(
                () ->
                        assertEquals(
                                result.getApplicant().getName().getTitle(),
                                personIdentity.getTitle()),
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
                                result.getLocationDetails().get(0).getUKLocation().getHouseName(),
                                personIdentity.getAddresses().get(0).getHouseName()),
                () ->
                        assertEquals(
                                result.getLocationDetails().get(0).getUKLocation().getFlat(),
                                personIdentity.getAddresses().get(0).getFlat()),
                () ->
                        assertEquals(
                                result.getLocationDetails().get(0).getUKLocation().getPostcode(),
                                personIdentity.getAddresses().get(0).getPostcode()),
                () ->
                        assertEquals(
                                result.getLocationDetails().get(0).getUKLocation().getPostTown(),
                                personIdentity.getAddresses().get(0).getTownCity()),
                () ->
                        assertEquals(
                                result.getLocationDetails().get(0).getUKLocation().getStreet(),
                                personIdentity.getAddresses().get(0).getStreet()));
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForPreviousAddress() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();

        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);

        assertAll(
                () -> assertEquals("urn", result.getControl().getURN()),
                () -> assertEquals("1 out of 2", result.getApplicationData().getProduct()),
                () ->
                        assertEquals(
                                personIdentity.getTitle(),
                                result.getApplicant().getName().getTitle()),
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
                                personIdentity.getAddresses().get(0).getHouseName(),
                                result.getLocationDetails().get(0).getUKLocation().getHouseName()),
                () ->
                        assertEquals(
                                personIdentity.getAddresses().get(0).getFlat(),
                                result.getLocationDetails().get(0).getUKLocation().getFlat()),
                () ->
                        assertEquals(
                                personIdentity.getAddresses().get(0).getPostcode(),
                                result.getLocationDetails().get(0).getUKLocation().getPostcode()),
                () ->
                        assertEquals(
                                personIdentity.getAddresses().get(0).getTownCity(),
                                result.getLocationDetails().get(0).getUKLocation().getPostTown()),
                () ->
                        assertEquals(
                                personIdentity.getAddresses().get(0).getStreet(),
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
