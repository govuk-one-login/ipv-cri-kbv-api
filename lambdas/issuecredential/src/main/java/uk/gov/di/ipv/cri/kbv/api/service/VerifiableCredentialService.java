package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.kbv.api.domain.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.*;

public class VerifiableCredentialService {

    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;

    private final ObjectMapper objectMapper;

    public VerifiableCredentialService() {
        this.configurationService = new ConfigurationService();
        this.signedJwtFactory =
                new SignedJWTFactory(
                        new KMSSigner(
                                configurationService.getVerifiableCredentialKmsSigningKeyId()));
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
    }

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    public SignedJWT generateSignedVerifiableCredentialJwt(
            String subject, PersonIdentity personIdentity, KBVItem kbvItem) throws JOSEException {
        var now = Instant.now();

        var claimsSet =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, subject)
                        .claim(ISSUER, configurationService.getVerifiableCredentialIssuer())
                        .claim(NOT_BEFORE, now.getEpochSecond())
                        .claim(
                                EXPIRATION_TIME,
                                now.plusSeconds(configurationService.getMaxJwtTtl())
                                        .getEpochSecond())
                        .claim(
                                VC_CLAIM,
                                Map.of(
                                        VC_TYPE,
                                        new String[] {
                                            VERIFIABLE_CREDENTIAL_TYPE, KBV_CREDENTIAL_TYPE
                                        },
                                        VC_CONTEXT,
                                        new String[] {W3_BASE_CONTEXT, DI_CONTEXT},
                                        VC_CREDENTIAL_SUBJECT,
                                        Map.of(
                                                VC_ADDRESS_KEY, personIdentity.getAddresses(),
                                                VC_NAME_KEY, generateName(personIdentity),
                                                VC_BIRTHDATE_KEY,
                                                        Map.of(
                                                                "value",
                                                                personIdentity
                                                                        .getDateOfBirth()
                                                                        .format(
                                                                                DateTimeFormatter
                                                                                        .ISO_DATE))),
                                        VC_EVIDENCE_KEY,
                                        calculateEvidence(kbvItem)))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet);
    }

    private Name generateName(PersonIdentity personIdentity) {
        Name name = new Name();

        NameParts givenName = new NameParts();
        givenName.setType("GivenName");
        givenName.setValue(personIdentity.getFirstName());

        NameParts familyName = new NameParts();
        familyName.setType("FamilyName");
        familyName.setValue(personIdentity.getSurname());

        name.setNameParts(List.of(new NameParts[] {givenName, familyName}));

        /// TODO: Deal with middle name(s)

        return name;
    }

    private Object[] calculateEvidence(KBVItem kbvItem) {

        /// TODO: Handle multiple evidence items

        Evidence evidence = new Evidence();
        evidence.setType(EvidenceType.IdentityCheck);
        evidence.setTxn(kbvItem.getAuthRefNo());

        /// TODO: calculate evidence score
        evidence.setVerificationScore(2);

        var evidenceObjects = new Object[1];
        evidenceObjects[0] = objectMapper.convertValue(evidence, Map.class);

        return evidenceObjects;
    }
}
