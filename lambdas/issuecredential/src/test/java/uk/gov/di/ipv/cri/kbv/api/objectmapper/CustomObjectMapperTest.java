package uk.gov.di.ipv.cri.kbv.api.objectmapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.kbv.api.domain.CheckDetail;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomObjectMapperTest implements TestFixtures {
    @Test
    void shouldConfigureObjectMapperCorrectly() {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        assertEquals(
                JsonInclude.Include.NON_NULL,
                objectMapper
                        .getSerializationConfig()
                        .getDefaultPropertyInclusion()
                        .getValueInclusion());
        assertEquals(3, objectMapper.getRegisteredModuleIds().size());
        assertFalse(
                objectMapper
                        .getSerializationConfig()
                        .isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void shouldSerializeJWTClaimsSetCorrectly() throws IOException {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .subject("subject")
                        .issuer("issuer")
                        .expirationTime(new Date(4070909400000L))
                        .build();

        String expectedJson = "{\"iss\":\"issuer\",\"sub\":\"subject\",\"exp\":4070909400}";

        String actualJson = objectMapper.writeValueAsString(claimsSet);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    void shouldSerializeWithCredentialSubject() throws IOException {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        CanonicalAddress address = new CanonicalAddress();
        address.setAddressCountry("GB");
        address.setBuildingName("");
        address.setStreetName("HADLEY ROAD");
        address.setPostalCode("BA2 5AA");
        address.setBuildingNumber("8");
        address.setAddressLocality("BATH");
        address.setPostalCode("BA2 5AA");

        address.setValidFrom(LocalDate.of(2000, 1, 1));
        Map<String, Object> vc = new HashMap<>();
        vc.put("credentialSubject", Map.of("address", List.of(address)));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("vc", vc).build();

        String actualJson = objectMapper.writeValueAsString(claimsSet);

        assertEquals(
                "{\"vc\":{\"credentialSubject\":{\"address\":[{\"addressCountry\":\"GB\",\"buildingName\":\"\",\"streetName\":\"HADLEY ROAD\",\"postalCode\":\"BA2 5AA\",\"buildingNumber\":\"8\",\"addressLocality\":\"BATH\",\"validFrom\":\"2000-01-01\"}]}}}",
                actualJson);
    }

    @Test
    void shouldSerializeWithVcCredentialSubjectEvidence() throws IOException {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        CanonicalAddress address = new CanonicalAddress();
        address.setAddressCountry("GB");
        address.setBuildingName("COY POND BUSINESS PARK");
        address.setSubBuildingName("UNIT 2B");
        address.setStreetName("HADLEY ROAD");
        address.setPostalCode("BA2 5AA");
        address.setBuildingNumber("8");
        address.setAddressLocality("BATH");
        address.setPostalCode("BA2 5AA");

        address.setValidFrom(LocalDate.of(2000, 1, 1));

        CheckDetail checkDetail = new CheckDetail();
        checkDetail.setKbvQuality(2);

        Evidence evidence = new Evidence();
        evidence.setCheckDetails(new CheckDetail[] {checkDetail});
        evidence.setFailedCheckDetails(
                new CheckDetail[] {new CheckDetail(), new CheckDetail(), new CheckDetail()});
        evidence.setTxn("dummyTxn");
        evidence.setVerificationScore(2);

        Map<String, Object> vc = new HashMap<>();
        Map<String, Object> vcClaim = new HashMap<>();

        vc.put("credentialSubject", Map.of("address", Collections.singletonList(address)));
        vcClaim.put("evidence", Collections.singletonList(evidence));
        vcClaim.put("@context", new String[] {"https://www.w3.org/2018/credentials/v1"});
        vcClaim.put("type", new String[] {"VerifiableCredential", "IdentityCheckCredential"});

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("vc", vc).build();

        String actualJson = objectMapper.writeValueAsString(claimsSet);

        assertEquals(
                "{\"vc\":{\"credentialSubject\":{\"address\":[{\"addressCountry\":\"GB\",\"buildingName\":\"COY POND BUSINESS PARK\",\"subBuildingName\":\"UNIT 2B\",\"streetName\":\"HADLEY ROAD\",\"postalCode\":\"BA2 5AA\",\"buildingNumber\":\"8\",\"addressLocality\":\"BATH\",\"validFrom\":\"2000-01-01\"}]}}}",
                actualJson);
    }
}
