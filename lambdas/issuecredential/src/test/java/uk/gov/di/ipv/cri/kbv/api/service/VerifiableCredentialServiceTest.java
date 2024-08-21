package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedBuilder;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;
import uk.gov.di.ipv.cri.kbv.api.domain.QuestionState;
import uk.gov.di.ipv.cri.kbv.api.service.fixtures.TestFixtures;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.EXPERIAN_IIQ_RESPONSE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.KBV_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_BIRTHDATE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_EVIDENCE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_NAME_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.W3_BASE_CONTEXT;

@ExtendWith(MockitoExtension.class)
class VerifiableCredentialServiceTest implements TestFixtures {
    private static final String SUBJECT = "subject";
    private static final JWTClaimsSet TEST_CLAIMS_SET =
            new JWTClaimsSet.Builder().subject("test").issuer("test").build();
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());

    @Mock private ConfigurationService mockConfigurationService;
    @Mock private VerifiableCredentialClaimsSetBuilder mockVcClaimSetBuilder;
    @Mock private EventProbe mockEventProbe;
    private EvidenceFactory spyEvidenceFactory;
    @Captor private ArgumentCaptor<Map<String, Object>[]> mapArrayArgumentCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;
    private VerifiableCredentialService verifiableCredentialService;
    @Mock private SessionItem mockSessionItem;
    private static final String ISSUER = "issuer";
    private static final String KMS_SIGNING_KEY_ID = "kmsSigningKeyId";

    @Nested
    class KbvVerifiableCredentialJwt implements TestFixtures {
        @ParameterizedTest
        @CsvSource({
            // 3 out of 4 prioritised
            "authenticated,3,3,0,2,",
            "authenticated,4,3,1,2,",
            "Not Authenticated,4,2,2,0,V03",
            "Not Authenticated,4,1,3,0,V03",
            "Not Authenticated,2,0,2,0,V03",
            "Unable to Authenticate,3,2,1,0,V03",
            // 2 out of 3 prioritised
            "authenticated,2,2,0,1,",
            "authenticated,3,2,1,1,",
            "Not Authenticated,3,1,2,0,V03",
            "Not Authenticated,2,0,2,0,V03",
            "Unable to Authenticate,2,1,1,0,V03",
        })
        void shouldReturnASignedVerifiableCredentialJwt(
                String authenticationResult,
                int totalQuestionsAsked,
                int answeredCorrectly,
                int answeredInCorrectly,
                int expectedVerificationScore,
                ContraIndicator expectedContraIndicator)
                throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                        ParseException, JsonProcessingException {
            initMockConfigurationService();
            SignedJWTFactory signedJwtFactory =
                    new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
            spyEvidenceFactory =
                    spy(
                            new EvidenceFactory(
                                    objectMapper,
                                    mockEventProbe,
                                    KBV_QUESTION_QUALITY_MAPPING_SERIALIZED));
            verifiableCredentialService =
                    new VerifiableCredentialService(
                            signedJwtFactory,
                            mockConfigurationService,
                            objectMapper,
                            mockVcClaimSetBuilder,
                            spyEvidenceFactory);
            EvidenceRequest evidenceRequest = new EvidenceRequest();
            evidenceRequest.setVerificationScore(expectedVerificationScore);
            mockSessionItem = new SessionItem();
            mockSessionItem.setSubject(SUBJECT);
            mockSessionItem.setEvidenceRequest(evidenceRequest);

            initMockVCClaimSetBuilder();

            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus(authenticationResult);
            kbvItem.setQuestionAnswerResultSummary(
                    getKbvQuestionAnswerSummary(
                            totalQuestionsAsked, answeredCorrectly, answeredInCorrectly));
            setKbvItemQuestionState(kbvItem);
            PersonIdentityDetailed personIdentity = createPersonIdentity();

            when(mockVcClaimSetBuilder.build()).thenReturn(TEST_CLAIMS_SET);

            SignedJWT signedJWT =
                    verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                            mockSessionItem, personIdentity, kbvItem);

            assertTrue(
                    signedJWT.verify(new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1))));
            verify(mockConfigurationService).getParameterValue("JwtTtlUnit");
            verify(mockConfigurationService).getMaxJwtTtl();
            verify(mockVcClaimSetBuilder).subject(SUBJECT);
            verify(mockVcClaimSetBuilder).verifiableCredentialType(KBV_CREDENTIAL_TYPE);
            verify(spyEvidenceFactory).create(kbvItem, mockSessionItem.getEvidenceRequest());

            makeEvidenceClaimsAssertions(
                    expectedVerificationScore, expectedContraIndicator, kbvItem.getAuthRefNo());

            makeVerifiableCredentialSubjectClaimsAssertions(personIdentity);
        }

        @Test
        void shouldExcludeAddressFilteredOutByAddressTypeWhenPresent()
                throws JOSEException, NoSuchAlgorithmException, JsonProcessingException {

            initMockConfigurationService();
            when(mockConfigurationService.getMaxJwtTtl()).thenReturn(10L);
            when(mockConfigurationService.getParameterValue("JwtTtlUnit")).thenReturn("MINUTES");
            when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(ISSUER);
            ArgumentCaptor<JWTClaimsSet> jwtClaimsSetArgumentCaptor =
                    ArgumentCaptor.forClass(JWTClaimsSet.class);
            SignedJWTFactory signedJWTFactory = mock(SignedJWTFactory.class);
            when(signedJWTFactory.createSignedJwt(
                            any(JWTClaimsSet.class), eq(ISSUER), eq(KMS_SIGNING_KEY_ID)))
                    .thenReturn(mock(SignedJWT.class));
            Clock clock = Clock.fixed(Instant.parse("2099-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
            VerifiableCredentialClaimsSetBuilder claimsSetBuilder =
                    new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);
            spyEvidenceFactory =
                    spy(
                            new EvidenceFactory(
                                    objectMapper,
                                    mockEventProbe,
                                    KBV_QUESTION_QUALITY_MAPPING_SERIALIZED));
            verifiableCredentialService =
                    new VerifiableCredentialService(
                            signedJWTFactory,
                            mockConfigurationService,
                            objectMapper,
                            claimsSetBuilder,
                            spyEvidenceFactory);

            EvidenceRequest evidenceRequest = new EvidenceRequest();
            mockSessionItem = new SessionItem();
            mockSessionItem.setSubject(SUBJECT);
            mockSessionItem.setEvidenceRequest(evidenceRequest);

            KBVItem kbvItem = getKbvItem();
            kbvItem.setStatus("not authenticated");
            setKbvItemQuestionState(kbvItem);

            String addressWithAddressTypeJson =
                    "{"
                            + "\"addressType\": \"CURRENT\","
                            + "\"buildingNumber\": \"114\","
                            + "\"streetName\": \"Wellington Street\","
                            + "\"postalCode\": \"LS1 1BA\""
                            + "}";

            Address address = objectMapper.readValue(addressWithAddressTypeJson, Address.class);
            var personIdentity = getPersonIdentityDetailedWithAddresses(List.of(address));

            verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                    mockSessionItem, personIdentity, kbvItem);

            verify(signedJWTFactory)
                    .createSignedJwt(
                            jwtClaimsSetArgumentCaptor.capture(),
                            eq(ISSUER),
                            eq(KMS_SIGNING_KEY_ID));

            JsonNode credentialSubject =
                    objectMapper
                            .readTree(jwtClaimsSetArgumentCaptor.getValue().toString())
                            .get("vc")
                            .get("credentialSubject");

            JsonNode nameParts = credentialSubject.get("name").get(0).get("nameParts");
            assertEquals("GivenName", nameParts.get(0).get("type").asText());
            assertEquals("FamilyName", nameParts.get(1).get("type").asText());
            assertEquals("Joe", nameParts.get(0).get("value").asText());
            assertEquals("Bloggs", nameParts.get(1).get("value").asText());

            JsonNode addressNode = credentialSubject.get("address").get(0);
            assertEquals("114", addressNode.get("buildingNumber").asText());
            assertEquals("Wellington Street", addressNode.get("streetName").asText());
            assertEquals("LS1 1BA", addressNode.get("postalCode").asText());
            assertNull(addressNode.get("addressType"));
        }

        @ParameterizedTest
        @CsvSource({"some-other-experian-status,an auth ref no,0", ",an auth ref no,0"})
        void shouldCreateValidSignedJWTWithVcZeroWhenKbvStatusNullOrAnyOtherValue(
                String status, String authRefNo, int expectedVerificationScore)
                throws JOSEException, JsonProcessingException, NoSuchAlgorithmException {
            SignedJWTFactory signedJWTFactory = mock(SignedJWTFactory.class);
            initMockVCClaimSetBuilder();
            initMockConfigurationService();

            spyEvidenceFactory =
                    spy(
                            new EvidenceFactory(
                                    objectMapper,
                                    mockEventProbe,
                                    KBV_QUESTION_QUALITY_MAPPING_SERIALIZED));
            verifiableCredentialService =
                    new VerifiableCredentialService(
                            signedJWTFactory,
                            mockConfigurationService,
                            objectMapper,
                            mockVcClaimSetBuilder,
                            spyEvidenceFactory);
            mockSessionItem = new SessionItem();
            mockSessionItem.setSubject(SUBJECT);

            KBVItem kbvItem = new KBVItem();
            kbvItem.setStatus(status);
            kbvItem.setAuthRefNo(authRefNo);
            QuestionState questionState = new QuestionState();
            kbvItem.setStatus("some unknown value");
            kbvItem.setQuestionState(objectMapper.writeValueAsString(questionState));

            when(mockVcClaimSetBuilder.build()).thenReturn(TEST_CLAIMS_SET);

            verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                    mockSessionItem, createPersonIdentity(), kbvItem);

            verify(mockVcClaimSetBuilder)
                    .verifiableCredentialEvidence(mapArrayArgumentCaptor.capture());
            verify(spyEvidenceFactory).create(kbvItem, null);
            Map<String, Object> evidenceItems = mapArrayArgumentCaptor.getValue()[0];
            assertEquals(kbvItem.getAuthRefNo(), evidenceItems.get("txn"));
            assertEquals(expectedVerificationScore, evidenceItems.get("verificationScore"));
        }
    }

    @Nested
    class KbvAuditEventExtensions implements TestFixtures {
        private final String txn = String.valueOf(UUID.randomUUID());

        @Test
        void shouldGetAuditEventExtensions() throws JsonProcessingException {
            SignedJWTFactory signedJWTFactory = mock(SignedJWTFactory.class);

            spyEvidenceFactory =
                    spy(
                            new EvidenceFactory(
                                    objectMapper,
                                    mockEventProbe,
                                    KBV_QUESTION_QUALITY_MAPPING_SERIALIZED));
            verifiableCredentialService =
                    new VerifiableCredentialService(
                            signedJWTFactory,
                            mockConfigurationService,
                            objectMapper,
                            mockVcClaimSetBuilder,
                            spyEvidenceFactory);

            KBVItem kbvItem = new KBVItem();
            kbvItem.setAuthRefNo("dummyTxn");
            kbvItem.setStatus("Not Authenticated");
            kbvItem.setQuestionAnswerResultSummary(getKbvQuestionAnswerSummary(2, 0, 2));
            setKbvItemQuestionState(kbvItem);

            var kbvQuestionResponseSummary =
                    Map.of(
                            "outcome", "Not Authenticated",
                            "totalQuestionsAnsweredCorrect", 0,
                            "totalQuestionsAsked", 2,
                            "totalQuestionsAnsweredIncorrect", 2);

            Object[] evidence = {
                Map.of(
                        "txn",
                        txn,
                        "verificationScore",
                        2,
                        "failedCheckDetails",
                        List.of(
                                Map.of("checkMethod", "kbv", "kbvResponseMode", "multiple_choice"),
                                Map.of("checkMethod", "kbv", "kbvResponseMode", "multiple_choice")),
                        "type",
                        "IdentityCheck",
                        EXPERIAN_IIQ_RESPONSE,
                        kbvQuestionResponseSummary)
            };

            when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(ISSUER);
            doReturn(evidence).when(spyEvidenceFactory).create(kbvItem, null);

            var auditEventExtensions =
                    verifiableCredentialService.getAuditEventExtensions(kbvItem, null);

            verify(spyEvidenceFactory).create(kbvItem, null);

            assertEquals(
                    auditEventExtensions,
                    Map.of(
                            JWTClaimNames.ISSUER,
                            ISSUER,
                            VC_EVIDENCE_KEY,
                            evidence,
                            EXPERIAN_IIQ_RESPONSE,
                            kbvQuestionResponseSummary));
        }
    }

    private PersonIdentityDetailed createPersonIdentity() {
        Address address = new Address();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");

        return getPersonIdentityDetailedWithAddresses(Collections.singletonList(address));
    }

    private PersonIdentityDetailed getPersonIdentityDetailedWithAddresses(List<Address> address) {
        Name name = new Name();
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Joe");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Bloggs");
        name.setNameParts(List.of(firstNamePart, surnamePart));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1980, 5, 3));

        return PersonIdentityDetailedBuilder.builder(List.of(name), List.of(birthDate))
                .withAddresses(address)
                .build();
    }

    private void initMockVCClaimSetBuilder() {
        when(mockVcClaimSetBuilder.subject(anyString())).thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.timeToLive(anyLong(), any(ChronoUnit.class)))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialEvidence(any()))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialSubject(any()))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialType(KBV_CREDENTIAL_TYPE))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialContext(
                        new String[] {W3_BASE_CONTEXT, DI_CONTEXT}))
                .thenReturn(mockVcClaimSetBuilder);
    }

    private void initMockConfigurationService() {
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(6L);
        when(mockConfigurationService.getParameterValue("JwtTtlUnit")).thenReturn("MONTHS");
        when(mockConfigurationService.getVerifiableCredentialKmsSigningKeyId())
                .thenReturn(KMS_SIGNING_KEY_ID);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(ISSUER);
    }

    private void makeEvidenceClaimsAssertions(
            int expectedVerificationScore,
            ContraIndicator expectedContraIndicator,
            String expectedAuthRefNo) {
        verify(mockVcClaimSetBuilder)
                .verifiableCredentialEvidence(mapArrayArgumentCaptor.capture());
        Map<String, Object> evidenceClaims = mapArrayArgumentCaptor.getValue()[0];
        assertEquals(expectedVerificationScore, evidenceClaims.get("verificationScore"));
        if (Objects.nonNull(expectedContraIndicator)) {
            assertEquals(
                    expectedContraIndicator.toString(),
                    ((List<?>) evidenceClaims.get("ci")).get(0));
        } else {
            assertNull(evidenceClaims.get("ci"));
        }
        assertEquals(expectedAuthRefNo, evidenceClaims.get("txn"));
    }

    private void makeVerifiableCredentialSubjectClaimsAssertions(
            PersonIdentityDetailed personIdentity) {
        verify(mockVcClaimSetBuilder).verifiableCredentialSubject(mapArgumentCaptor.capture());
        Map<String, Object> vcSubjectClaims = mapArgumentCaptor.getValue();
        assertEquals(personIdentity.getNames(), vcSubjectClaims.get(VC_NAME_KEY));

        Object birthDateMap = ((Object[]) vcSubjectClaims.get(VC_BIRTHDATE_KEY))[0];
        assertEquals(
                personIdentity.getBirthDates().get(0).getValue().format(DateTimeFormatter.ISO_DATE),
                ((Map<?, ?>) birthDateMap).get("value"));

        Map<?, ?> vcSubjectAddressMap =
                (Map<?, ?>) ((Object[]) vcSubjectClaims.get(VC_ADDRESS_KEY))[0];
        Address personIdentityAddress = personIdentity.getAddresses().get(0);
        assertEquals(personIdentityAddress.getStreetName(), vcSubjectAddressMap.get("streetName"));
        assertEquals(personIdentityAddress.getPostalCode(), vcSubjectAddressMap.get("postalCode"));
        assertEquals(
                personIdentityAddress.getBuildingNumber(),
                vcSubjectAddressMap.get("buildingNumber"));
        assertFalse(vcSubjectAddressMap.containsKey("addressType"));
    }
}
