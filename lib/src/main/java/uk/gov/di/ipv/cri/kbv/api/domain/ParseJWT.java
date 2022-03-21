package uk.gov.di.ipv.cri.kbv.api.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParseJWT {

    public static final String CLAIMS = "claims";
    public static final String VC_HTTP_API = "vc_http_api";
    private final ObjectMapper objectMapper;

    public ParseJWT(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<PersonIdentity> getPersonIdentity(String jwt)
            throws ParseException, JsonProcessingException {
        try {
            SignedJWT jwtString = SignedJWT.parse(jwt);
            String payload = jwtString.getJWTClaimsSet().getClaim(CLAIMS).toString();
            JsonNode jsonNode = objectMapper.readTree(payload).get(VC_HTTP_API);
            PersonIdentitySharedAttribute personIdentitySharedAttribute =
                    objectMapper.readValue(jsonNode.toString(), PersonIdentitySharedAttribute.class);
            String personIdentity = from(personIdentitySharedAttribute);
            return Optional.ofNullable(
                    objectMapper.readValue(personIdentity, PersonIdentity.class));
        } catch (ParseException | JsonProcessingException e) {
            throw e;
        }
    }

    public String from(PersonIdentitySharedAttribute personIdentitySharedAttribute) throws JsonProcessingException {
        PersonIdentity identity = new PersonIdentity();
        identity.setDateOfBirth(
                LocalDate.parse(personIdentitySharedAttribute.getDatesOfBirth().get(0)));
        identity.setAddresses(mapAddresses(personIdentitySharedAttribute.getUKAddresses()));
        identity.setFirstName(personIdentitySharedAttribute.getNames().get(0).getFirstName());
        identity.setSurname(personIdentitySharedAttribute.getNames().get(0).getSurname());
        return objectMapper.writeValueAsString(identity);
    }

    private List<PersonAddress> mapAddresses(List<UKAddresses> ukPersonAddresses) {
        List<PersonAddress> addresses = new ArrayList<>();
        ukPersonAddresses.forEach(
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
                    addresses.add(address);
                });

        return addresses;
    }
}
