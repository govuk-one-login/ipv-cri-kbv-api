package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.KBV_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_BIRTHDATE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_EVIDENCE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_NAME_KEY;

public class VerifiableCredentialService {
    private final VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder;
    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;
    private final EvidenceFactory evidenceFactory;

    @ExcludeFromGeneratedCoverageReport
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
        this.evidenceFactory =
                new EvidenceFactory(this.objectMapper, new EventProbe());
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
            String subject, PersonIdentityDetailed personIdentity, KBVItem kbvItem)
            throws JOSEException {
        long jwtTtl = this.configurationService.getMaxJwtTtl();
        ChronoUnit jwtTtlUnit =
                ChronoUnit.valueOf(this.configurationService.getParameterValue("JwtTtlUnit"));
        var claimsSet =
                this.vcClaimsSetBuilder
                        .subject(subject)
                        .timeToLive(jwtTtl, jwtTtlUnit)
                        .verifiableCredentialType(KBV_CREDENTIAL_TYPE)
                        .verifiableCredentialSubject(
                                Map.of(
                                        VC_ADDRESS_KEY,
                                        convertAddresses(personIdentity.getAddresses()),
                                        VC_NAME_KEY,
                                        personIdentity.getNames(),
                                        VC_BIRTHDATE_KEY,
                                        convertBirthDates(personIdentity.getBirthDates())))
                        .verifiableCredentialEvidence(
                                evidenceFactory.create(kbvItem))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet);
    }

    public Map<String, Object> getAuditEventExtensions(KBVItem kbvItem) {
        return Map.of(
                ISSUER,
                configurationService.getVerifiableCredentialIssuer(),
                VC_EVIDENCE_KEY,
                evidenceFactory.create(kbvItem));
    }

    @SuppressWarnings("unchecked")
    private Object[] convertAddresses(List<Address> addresses) {
        return addresses.stream()
                .map(
                        address -> {
                            var mappedAddress = objectMapper.convertValue(address, Map.class);
                            // Skip superfluous address type from the map to match RFC
                            HashMap<String, Object> addressMap = new HashMap<>();
                            if (mappedAddress != null) {
                                mappedAddress.forEach(
                                        (key, value) -> {
                                            if (!key.equals("addressType")) {
                                                addressMap.put(key.toString(), value);
                                            }
                                        });
                            }

                            return addressMap;
                        })
                .toArray();
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
