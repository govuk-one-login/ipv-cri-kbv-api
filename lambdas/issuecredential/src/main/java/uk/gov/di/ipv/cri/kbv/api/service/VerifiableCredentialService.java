package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.EXPERIAN_IIQ_RESPONSE;
import static uk.gov.di.ipv.cri.kbv.api.domain.KbvResponsesAuditExtension.createAuditEventExtensions;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.KBV_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_BIRTHDATE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_EVIDENCE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_NAME_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.W3_BASE_CONTEXT;

public class VerifiableCredentialService {
    private final VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder;
    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;
    private final EvidenceFactory evidenceFactory;

    @ExcludeFromGeneratedCoverageReport
    public VerifiableCredentialService(
            ConfigurationService configurationService, KmsClient kmsClient)
            throws JsonProcessingException {
        this.configurationService = configurationService;
        this.signedJwtFactory =
                new SignedJWTFactory(
                        new KMSSigner(
                                configurationService.getVerifiableCredentialKmsSigningKeyId(),
                                kmsClient));
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
        var kbvQualitySecretValue =
                configurationService.getParameterValueByAbsoluteName(
                        "/kbv-cri-api-v1/quality/mappings");
        final Map<String, Integer> kbvQualityMapping =
                objectMapper.readValue(kbvQualitySecretValue, Map.class);
        this.evidenceFactory =
                new EvidenceFactory(this.objectMapper, new EventProbe(), kbvQualityMapping);
        this.vcClaimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(
                        this.configurationService, Clock.systemUTC());
    }

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper,
            VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder,
            EvidenceFactory evidenceFactory) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
        this.evidenceFactory = evidenceFactory;
        this.vcClaimsSetBuilder = vcClaimsSetBuilder;
    }

    @Tracing
    public SignedJWT generateSignedVerifiableCredentialJwt(
            SessionItem sessionItem, PersonIdentityDetailed personIdentity, KBVItem kbvItem)
            throws JOSEException, NoSuchAlgorithmException, JsonProcessingException {
        long jwtTtl = this.configurationService.getMaxJwtTtl();
        String issuer = configurationService.getVerifiableCredentialIssuer();
        String kmsSigningKeyId = configurationService.getVerifiableCredentialKmsSigningKeyId();
        ChronoUnit jwtTtlUnit =
                ChronoUnit.valueOf(this.configurationService.getParameterValue("JwtTtlUnit"));
        var claimsSet =
                this.vcClaimsSetBuilder
                        .subject(sessionItem.getSubject())
                        .timeToLive(jwtTtl, jwtTtlUnit)
                        .verifiableCredentialType(KBV_CREDENTIAL_TYPE)
                        .verifiableCredentialContext(new String[] {W3_BASE_CONTEXT, DI_CONTEXT})
                        .verifiableCredentialSubject(
                                Map.of(
                                        VC_ADDRESS_KEY,
                                        convertAddresses(personIdentity.getAddresses()),
                                        VC_NAME_KEY,
                                        personIdentity.getNames(),
                                        VC_BIRTHDATE_KEY,
                                        convertBirthDates(personIdentity.getBirthDates())))
                        .verifiableCredentialEvidence(
                                evidenceFactory.create(kbvItem, sessionItem.getEvidenceRequest()))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet, issuer, kmsSigningKeyId);
    }

    public Map<String, Object> getAuditEventExtensions(
            KBVItem kbvItem, EvidenceRequest evidenceRequest) throws JsonProcessingException {
        return Map.of(
                ISSUER,
                configurationService.getVerifiableCredentialIssuer(),
                VC_EVIDENCE_KEY,
                evidenceFactory.create(kbvItem, evidenceRequest),
                EXPERIAN_IIQ_RESPONSE,
                createAuditEventExtensions(
                        kbvItem.getStatus(), kbvItem.getQuestionAnswerResultSummary()));
    }

    @SuppressWarnings("unchecked")
    private Object[] convertAddresses(List<Address> addresses) {
        return addresses.stream().map(this::convertAddressToMapAndFilter).toArray();
    }

    private Map<String, Object> convertAddressToMapAndFilter(Address address) {
        // Skip superfluous address type from the map to match RFC
        ObjectNode addressNode = objectMapper.convertValue(address, ObjectNode.class);
        addressNode.remove("addressType");

        return objectMapper.convertValue(addressNode, LinkedHashMap.class);
    }

    private Object[] convertBirthDates(List<BirthDate> birthDates) {
        return birthDates.stream()
                .map(
                        birthDate ->
                                Map.of(
                                        "value",
                                        birthDate
                                                .getValue()
                                                .format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .toArray();
    }
}
