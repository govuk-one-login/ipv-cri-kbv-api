package uk.gov.di.ipv.cri.kbv.api.library.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParseJWT {

    public static final String SHARED_CLAIMS = "shared_claims";
    private final ObjectMapper objectMapper;

    public ParseJWT(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<PersonIdentity> getPersonIdentity(String jwt)
            throws ParseException, JsonProcessingException {
        SignedJWT jwtString = SignedJWT.parse(jwt);
        String payload = jwtString.getJWTClaimsSet().getClaim(SHARED_CLAIMS).toString();
        SharedClaims sharedClaims = objectMapper.readValue(payload, SharedClaims.class);
        String personIdentity = from(sharedClaims);
        return Optional.ofNullable(objectMapper.readValue(personIdentity, PersonIdentity.class));
    }

    private String from(SharedClaims sharedClaims) throws JsonProcessingException {
        PersonIdentity identity = new PersonIdentity();
        identity.setDateOfBirth(LocalDate.parse(sharedClaims.getBirthDate().get(0).getValue()));
        identity.setAddresses(mapAddresses(sharedClaims.getUkAddresses()));
        identity.setFirstName(sharedClaims.getNames().get(0).getNameParts().get(0).getValue());
        identity.setSurname(sharedClaims.getNames().get(0).getNameParts().get(1).getValue());
        return objectMapper.writeValueAsString(identity);
    }

    private List<PersonAddress> mapAddresses(List<UKAddresses> ukPersonAddresses) {
        return ukPersonAddresses.stream()
                .map(
                        ukPersonAddress -> {
                            PersonAddress address = new PersonAddress();
                            address.setHouseNumber(ukPersonAddress.getStreet1());
                            address.setStreet(ukPersonAddress.getStreet2());
                            address.setTownCity(ukPersonAddress.getTownCity());
                            address.setPostcode(ukPersonAddress.getPostCode());
                            address.setAddressType(
                                    ukPersonAddress.isCurrentAddress()
                                            ? AddressType.CURRENT
                                            : AddressType.PREVIOUS);
                            return address;
                        })
                .collect(Collectors.toList());
    }
}
