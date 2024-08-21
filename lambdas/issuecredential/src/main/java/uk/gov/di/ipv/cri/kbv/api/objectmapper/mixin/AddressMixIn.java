package uk.gov.di.ipv.cri.kbv.api.objectmapper.mixin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@JsonPropertyOrder({
    "addressCountry",
    "uprn",
    "buildingName",
    "subBuildingName",
    "organisationName",
    "streetName",
    "dependentStreetName",
    "postalCode",
    "buildingNumber",
    "dependentAddressLocality",
    "addressLocality",
    "doubleDependentAddressLocality"
})
@ExcludeFromGeneratedCoverageReport
public abstract class AddressMixIn {}
