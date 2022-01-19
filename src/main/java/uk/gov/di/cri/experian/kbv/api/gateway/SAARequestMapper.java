package uk.gov.di.cri.experian.kbv.api.gateway;

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
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import spark.utils.StringUtils;
import uk.gov.di.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.cri.experian.kbv.api.domain.QuestionsResponse;

import java.util.UUID;

public class SAARequestMapper {

    public SAARequest mapPersonIdentity(PersonIdentity personIdentity) {
        return createRequest(personIdentity);
    }

    private SAARequest createRequest(PersonIdentity personIdentity) {
        SAARequest saaRequest = new SAARequest();
        setApplicant(saaRequest, personIdentity);
        setApplicationData(saaRequest);
        setControl(saaRequest);
        setLocationDetails(saaRequest, personIdentity);
        setResidency(saaRequest);
        return saaRequest;
    }

    private void setResidency(SAARequest saaRequest) {
        Residency residency = new Residency();
        residency.setApplicantIdentifier(1);
        residency.setLocationIdentifier(1);
        saaRequest.getResidency().add(residency);
    }

    private void setApplicationData(SAARequest saaRequest) {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setApplicationType("IG");
        applicationData.setChannel("IN");
        applicationData.setSearchConsent("Y");
        applicationData.setProduct("3 out of 4");
        saaRequest.setApplicationData(applicationData);
    }

    private void setControl(SAARequest saaRequest) {
        saaRequest.setControl(createControl());
    }

    private Control createControl() {
        Control control = new Control();
        control.setTestDatabase("A");
        Parameters parameters = new Parameters();
        parameters.setOneShotAuthentication("N");
        parameters.setStoreCaseData("P");
        control.setParameters(parameters);
        control.setURN(UUID.randomUUID().toString());
        control.setOperatorID("GDSCABINETUIIQ01U");
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

        if (StringUtils.isNotBlank(personIdentity.getTitle())) {
            name.setTitle(personIdentity.getTitle());
        }

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
        LocationDetails locationDetails = new LocationDetails();
        locationDetails.setLocationIdentifier(1);
        LocationDetailsUKLocation ukLocation = new LocationDetailsUKLocation();

        if (personIdentity.getAddresses() != null && personIdentity.getAddresses().size() > 0) {
            if (StringUtils.isNotBlank(personIdentity.getAddresses().get(0).getHouseName())) {
                ukLocation.setHouseName(personIdentity.getAddresses().get(0).getHouseName());
            }

            if (StringUtils.isNotBlank(personIdentity.getAddresses().get(0).getHouseNumber())) {
                ukLocation.setHouseNumber(personIdentity.getAddresses().get(0).getHouseNumber());
            }

            if (StringUtils.isNotBlank(personIdentity.getAddresses().get(0).getFlat())) {
                ukLocation.setFlat(personIdentity.getAddresses().get(0).getFlat());
            }

            if (StringUtils.isNotBlank(personIdentity.getAddresses().get(0).getDistrict())) {
                ukLocation.setDistrict(personIdentity.getAddresses().get(0).getDistrict());
            }
        }

        ukLocation.setPostcode(personIdentity.getAddresses().get(0).getPostcode());
        ukLocation.setPostTown(personIdentity.getAddresses().get(0).getTownCity());
        ukLocation.setStreet(personIdentity.getAddresses().get(0).getStreet());
        locationDetails.setUKLocation(ukLocation);
        //        locationDetails.setClientLocationID("1");
        saaRequest.getLocationDetails().add(locationDetails);
    }

    public QuestionsResponse mapSAAResponse2ToQuestionsResponse(SAAResponse2 sAAResponse2) {
        QuestionsResponse questionsResponse = new QuestionsResponse();
        questionsResponse.setQuestions(sAAResponse2.getQuestions());
        questionsResponse.setControl(sAAResponse2.getControl());
        questionsResponse.setError(sAAResponse2.getError());
        questionsResponse.setResults(sAAResponse2.getResults());
        return questionsResponse;
    }
}
