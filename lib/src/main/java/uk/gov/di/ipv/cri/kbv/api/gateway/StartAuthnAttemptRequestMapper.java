package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.Applicant;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicantDateOfBirth;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicantName;
import com.experian.uk.schema.experian.identityiq.services.webservice.ApplicationData;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetails;
import com.experian.uk.schema.experian.identityiq.services.webservice.LocationDetailsUKLocation;
import com.experian.uk.schema.experian.identityiq.services.webservice.Parameters;
import com.experian.uk.schema.experian.identityiq.services.webservice.Residency;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionRequest;
import uk.gov.di.ipv.cri.kbv.api.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class StartAuthnAttemptRequestMapper {

    private static final String DEFAULT_TITLE = "MR";
    private static final String IIQ_DATABASE_MODE_PARAM_NAME = "IIQDatabaseMode";

    private final ConfigurationService configurationService;

    StartAuthnAttemptRequestMapper(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    SAARequest mapQuestionRequest(QuestionRequest questionRequest) {
        Objects.requireNonNull(questionRequest, "The QuestionRequest must not be null");

        return createRequest(questionRequest);
    }

    private SAARequest createRequest(QuestionRequest questionRequest) {
        SAARequest saaRequest = new SAARequest();
        setApplicant(saaRequest, questionRequest.getPersonIdentity());
        setApplicationData(saaRequest, questionRequest.getStrategy());
        setControl(saaRequest, questionRequest);
        setLocationDetails(saaRequest, questionRequest.getPersonIdentity());
        setResidency(saaRequest);
        return saaRequest;
    }

    private void setResidency(SAARequest saaRequest) {
        Residency residency = new Residency();
        residency.setApplicantIdentifier(1);
        residency.setLocationIdentifier(1);
        saaRequest.getResidency().add(residency);
    }

    private void setApplicationData(SAARequest saaRequest, String strategy) {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setApplicationType("IG");
        applicationData.setChannel("IN");
        applicationData.setSearchConsent("Y");
        applicationData.setProduct(strategy);
        saaRequest.setApplicationData(applicationData);
    }

    private void setControl(SAARequest saaRequest, QuestionRequest questionRequest) {
        saaRequest.setControl(createControl(questionRequest));
    }

    private Control createControl(QuestionRequest questionRequest) {
        Control control = new Control();
        control.setTestDatabase(
                configurationService.getParameterValue(IIQ_DATABASE_MODE_PARAM_NAME));
        Parameters parameters = new Parameters();
        parameters.setOneShotAuthentication("N");
        parameters.setStoreCaseData("P");
        control.setParameters(parameters);
        control.setURN(
                StringUtils.isNotBlank(questionRequest.getUrn())
                        ? questionRequest.getUrn()
                        : UUID.randomUUID().toString());
        control.setOperatorID(questionRequest.getIiqOperatorId());
        return control;
    }

    private void setApplicant(SAARequest saaRequest, PersonIdentity personIdentity) {
        Applicant applicant = new Applicant();
        applicant.setApplicantIdentifier("1");
        ApplicantName name = new ApplicantName();

        if (StringUtils.isNotBlank(personIdentity.getFirstName())) {
            name.setForename(personIdentity.getFirstName());
        }

        if (StringUtils.isNotBlank(personIdentity.getSurname())) {
            name.setSurname(personIdentity.getSurname());
        }

        name.setTitle(DEFAULT_TITLE);
        applicant.setName(name);

        ApplicantDateOfBirth dateOfBirth = new ApplicantDateOfBirth();

        if (personIdentity.getDateOfBirth() != null) {
            dateOfBirth.setDD(personIdentity.getDateOfBirth().getDayOfMonth());
            dateOfBirth.setMM(personIdentity.getDateOfBirth().getMonth().getValue());
            dateOfBirth.setCCYY(personIdentity.getDateOfBirth().getYear());
        }
        applicant.setDateOfBirth(dateOfBirth);

        saaRequest.setApplicant(applicant);
    }

    private void setLocationDetails(SAARequest saaRequest, PersonIdentity personIdentity) {
        List<Address> addresses =
                Objects.requireNonNull(
                        personIdentity.getAddresses(), "personIdentity must have addresses");
        AtomicInteger locationIdentifier = new AtomicInteger(1);
        addresses.stream()
                .filter(address -> address.getValidFrom() != null)
                .map(personAddress -> mapAddressToLocation(locationIdentifier, personAddress))
                .forEach(locationDetails -> saaRequest.getLocationDetails().add(locationDetails));
    }

    private LocationDetails mapAddressToLocation(
            AtomicInteger locationIdentifier, Address personAddress) {
        LocationDetailsUKLocation ukLocation = new LocationDetailsUKLocation();

        if (StringUtils.isNotBlank(personAddress.getBuildingName())) {
            ukLocation.setHouseName(personAddress.getBuildingName());
        }

        if (StringUtils.isNotBlank(personAddress.getBuildingNumber())) {
            ukLocation.setHouseNumber(personAddress.getBuildingNumber());
        }

        ukLocation.setPostcode(personAddress.getPostalCode());
        ukLocation.setPostTown(personAddress.getAddressLocality());
        ukLocation.setStreet(personAddress.getStreetName());

        LocationDetails locationDetails = new LocationDetails();
        locationDetails.setLocationIdentifier(locationIdentifier.getAndIncrement());
        locationDetails.setUKLocation(ukLocation);

        return locationDetails;

        // TODO: examine the edge cases, specifically when the
        // following fields are populated:
        // personAddress.getDependentAddressLocality() &
        // personAddress.getDoubleDependentAddressLocality()
    }
}
