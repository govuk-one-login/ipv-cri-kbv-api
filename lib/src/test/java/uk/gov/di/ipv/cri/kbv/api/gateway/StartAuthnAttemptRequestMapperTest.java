package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetails;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.service.EnvironmentVariablesService;
import uk.gov.di.ipv.cri.kbv.api.service.MetricsService;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequest;
import static uk.gov.di.ipv.cri.kbv.api.util.TestDataCreator.createTestQuestionAnswerRequestWithDuplicateAddresses;

@ExtendWith(MockitoExtension.class)
class StartAuthnAttemptRequestMapperTest {
    private static final String TEST_STACK_NAME = "stack-name";
    private static final String PARAM_NAME_FORMAT = "/%s/%s";
    private StartAuthnAttemptRequestMapper startAuthnAttemptRequestMapper;
    QuestionRequest questionRequest;
    @Mock private MetricsService metricsService;
    @Mock private EnvironmentVariablesService environmentVariablesService;
    @Mock private SSMProvider mockSsmProvider;
    @Mock private SecretsProvider mockSecretsProvider;
    @Mock private Clock mockClock;

    @BeforeEach
    void setUp() {
        startAuthnAttemptRequestMapper =
                new StartAuthnAttemptRequestMapper(
                        new ConfigurationService(
                                mockSsmProvider, mockSecretsProvider, TEST_STACK_NAME, mockClock),
                        metricsService,
                        environmentVariablesService);
    }

    @Test
    void shouldSendMetrics() {
        SAAResponse2 response = mock(SAAResponse2.class);
        Results results = mock(Results.class);
        when(response.getResults()).thenReturn(results);
        Error error = mock(Error.class);
        when(response.getError()).thenReturn(error);
        startAuthnAttemptRequestMapper.mapSAAResponse2ToQuestionsResponse(response);
        verify(metricsService).sendResultMetric(results, "initial_questions_response");
        verify(metricsService).sendErrorMetric(error, "initial_questions_response_error");
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
        String fullParamName = String.format(PARAM_NAME_FORMAT, TEST_STACK_NAME, "IIQDatabaseMode");
        when(mockSsmProvider.get(fullParamName)).thenReturn("Static");
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

    @Test
    void shouldSetGenderIfGenderEnvVarPresent() {
        questionRequest = createTestQuestionAnswerRequest(AddressType.CURRENT);
        when(environmentVariablesService.getEnvironmentVariable(EnvironmentVariablesService.GENDER))
                .thenReturn(Optional.of("F"));
        SAARequest saaRequest = startAuthnAttemptRequestMapper.mapQuestionRequest(questionRequest);
        assertThat(saaRequest.getApplicant().getGender(), equalTo("F"));
        verify(environmentVariablesService)
                .getEnvironmentVariable(EnvironmentVariablesService.GENDER);
    }
}
