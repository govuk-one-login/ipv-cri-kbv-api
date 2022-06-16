package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_BIRTHDATE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_EVIDENCE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;

@ExtendWith(MockitoExtension.class)
class VerifiableCredentialServiceTest implements TestFixtures {
    private static final String SUBJECT = "subject";
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setUp() {
        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("https://kbv-cri.account.gov.uk.TBC");
    }

    @Test
    void shouldReturnAVerifiedCredentialWithSuccessScoreOnAuthorised()
            throws JOSEException, ParseException, JsonProcessingException, InvalidKeySpecException,
                    NoSuchAlgorithmException {

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
        var verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory, mockConfigurationService, mockObjectMapper);

        when(mockObjectMapper.convertValue(any(Evidence.class), eq(Map.class)))
                .thenReturn(Map.of("verificationScore", 2));
        when(mockObjectMapper.convertValue(any(Address.class), eq(Map.class)))
                .thenReturn(Map.of("address", new Address()));

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus(VC_THIRD_PARTY_KBV_CHECK_PASS);

        PersonIdentityDetailed personIdentity = createPersonIdentity();

        SignedJWT signedJWT =
                verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, personIdentity, kbvItem);
        JWTClaimsSet generatedClaims = signedJWT.getJWTClaimsSet();
        assertTrue(signedJWT.verify(new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1))));

        JsonNode claimsSet = new ObjectMapper().readTree(generatedClaims.toString());

        assertEquals(5, claimsSet.size());

        assertAll(
                () -> {
                    assertEquals(
                            personIdentity
                                    .getBirthDates()
                                    .get(0)
                                    .getValue()
                                    .format(DateTimeFormatter.ISO_DATE),
                            claimsSet
                                    .get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_BIRTHDATE_KEY)
                                    .get(0)
                                    .get("value")
                                    .asText());

                    assertEquals(
                            VC_PASS_EVIDENCE_SCORE,
                            claimsSet
                                    .get(VC_CLAIM)
                                    .get(VC_EVIDENCE_KEY)
                                    .get(0)
                                    .get("verificationScore")
                                    .asInt());
                });
        ECDSAVerifier ecVerifier = new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1));
        assertTrue(signedJWT.verify(ecVerifier));
    }

    @Test
    void shouldReturnAVerifiedCredentialWithFailScoreOnNotAuthorised()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException, ParseException,
                    JsonProcessingException {
        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
        VerifiableCredentialService verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory, mockConfigurationService, mockObjectMapper);
        when(mockObjectMapper.convertValue(any(Evidence.class), eq(Map.class)))
                .thenReturn(Map.of("verificationScore", 0));

        when(mockObjectMapper.convertValue(any(Address.class), eq(Map.class)))
                .thenReturn(Map.of("address", new Address()));

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setAuthRefNo(UUID.randomUUID().toString());
        kbvItem.setStatus(VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED);

        PersonIdentityDetailed personIdentity = createPersonIdentity();

        SignedJWT signedJWT =
                verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, personIdentity, kbvItem);
        JWTClaimsSet generatedClaims = signedJWT.getJWTClaimsSet();
        assertTrue(signedJWT.verify(new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1))));

        JsonNode claimsSet = new ObjectMapper().readTree(generatedClaims.toString());

        assertEquals(5, claimsSet.size());

        assertAll(
                () -> {
                    assertEquals(
                            personIdentity
                                    .getBirthDates()
                                    .get(0)
                                    .getValue()
                                    .format(DateTimeFormatter.ISO_DATE),
                            claimsSet
                                    .get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_BIRTHDATE_KEY)
                                    .get(0)
                                    .get("value")
                                    .asText());

                    assertEquals(
                            VC_FAIL_EVIDENCE_SCORE,
                            claimsSet
                                    .get(VC_CLAIM)
                                    .get(VC_EVIDENCE_KEY)
                                    .get(0)
                                    .get("verificationScore")
                                    .asInt());
                });
        ECDSAVerifier ecVerifier = new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1));
        assertTrue(signedJWT.verify(ecVerifier));
    }

    @Test
    void shouldCreateValidSignedJWT() throws JOSEException {

        SignedJWTFactory mockSignedClaimSetJwt = mock(SignedJWTFactory.class);
        var verifiableCredentialService =
                new VerifiableCredentialService(
                        mockSignedClaimSetJwt, mockConfigurationService, mockObjectMapper);

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("kbv-cri-issue");
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(342L);

        KBVItem kbvItem = new KBVItem();
        kbvItem.setSessionId(UUID.randomUUID());
        kbvItem.setExpiryDate(Instant.now().plusSeconds(342).getEpochSecond());
        kbvItem.setStatus(VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED);

        PersonIdentityDetailed personIdentity = createPersonIdentity();

        verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                SUBJECT, personIdentity, kbvItem);

        verify(mockSignedClaimSetJwt).createSignedJwt(any());
    }

    private PersonIdentityDetailed createPersonIdentity() {
        Address address = new Address();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");

        Name name = new Name();
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Bloggs");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Bloggs");
        name.setNameParts(List.of(firstNamePart, surnamePart));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1980, 5, 3));

        return new PersonIdentityDetailed(List.of(name), List.of(birthDate), List.of(address));
    }
}
