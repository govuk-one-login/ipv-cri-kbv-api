package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetails;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequest;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequestWithDuplicateAddresses;

@ExtendWith(MockitoExtension.class)
class StartAuthnAttemptRequestMapperTest {
    private StartAuthnAttemptRequestMapper startAuthnAttemptRequestMapper;
    private QuestionRequest questionRequest;
    @Mock private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setUp() {
        startAuthnAttemptRequestMapper =
                new StartAuthnAttemptRequestMapper(mockConfigurationService);
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForCurrentAddress() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();
        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);
        Address personAddress = personIdentity.getAddresses().get(0);
        assertAll(
                () -> assertEquals("MR", result.getApplicant().getName().getTitle()),
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
    void shouldOnlyMapAddressWithAValidFromValue() {
        questionRequest =
                createTestQuestionAnswerRequestWithDuplicateAddresses(AddressType.CURRENT);
        assertEquals(4, questionRequest.getPersonIdentity().getAddresses().size());
        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);
        List<LocationDetails> locationDetails = result.getLocationDetails();
        assertEquals(2, locationDetails.size());
    }

    @Test
    void shouldConvertPersonIdentityToSAARequestForPreviousAddress() {
        when(mockConfigurationService.getParameterValue("IIQDatabaseMode")).thenReturn("Static");
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        PersonIdentity personIdentity = questionRequest.getPersonIdentity();

        SAARequest result = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);

        assertNotNull(result);
        Address personAddress = personIdentity.getAddresses().get(0);
        assertAll(
                () -> assertEquals("Static", result.getControl().getTestDatabase()),
                () -> assertEquals("urn", result.getControl().getURN()),
                () -> assertEquals("1 out of 2", result.getApplicationData().getProduct()),
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
