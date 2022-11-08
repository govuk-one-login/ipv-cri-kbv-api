package uk.gov.di.ipv.cri.kbv.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import uk.gov.di.ipv.cri.kbv.api.domain.ContraIndicator;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;
import uk.gov.di.ipv.cri.kbv.api.domain.KBVItem;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.KBV_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_BIRTHDATE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_EVIDENCE_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_FAIL_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_NAME_KEY;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_PASS_EVIDENCE_SCORE;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED;
import static uk.gov.di.ipv.cri.kbv.api.domain.VerifiableCredentialConstants.VC_THIRD_PARTY_KBV_CHECK_PASS;

public class VerifiableCredentialService {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String METRIC_DIMENSION_KBV_VERIFICATION = "kbv_verification";

    private final VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder;
    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;
    private final EventProbe eventProbe;

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
        this.eventProbe = new EventProbe();
        this.vcClaimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(
                        this.configurationService, Clock.systemUTC());
    }

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper,
            EventProbe eventProbe,
            VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
        this.eventProbe = eventProbe;
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
                        .verifiableCredentialEvidence(calculateEvidence(kbvItem))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet);
    }

    public Map<String, Object> getAuditEventExtensions(KBVItem kbvItem) {
        return Map.of(
                ISSUER,
                configurationService.getVerifiableCredentialIssuer(),
                VC_EVIDENCE_KEY,
                calculateEvidence(kbvItem));
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

    private Object[] calculateEvidence(KBVItem kbvItem) {
        Evidence evidence = new Evidence();
        evidence.setTxn(kbvItem.getAuthRefNo());
        if (hasMultipleIncorrectAnswers(kbvItem)) {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            evidence.setCi(new ContraIndicator[] {ContraIndicator.V03});
            logVcScore("fail");
        } else if (VC_THIRD_PARTY_KBV_CHECK_PASS.equalsIgnoreCase(kbvItem.getStatus())) {
            evidence.setVerificationScore(VC_PASS_EVIDENCE_SCORE);
            logVcScore("pass");
        } else {
            evidence.setVerificationScore(VC_FAIL_EVIDENCE_SCORE);
            logVcScore("fail");
        }

        return new Map[] {objectMapper.convertValue(evidence, Map.class)};
    }

    private boolean hasMultipleIncorrectAnswers(KBVItem kbvItem) {
        return VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED.equalsIgnoreCase(kbvItem.getStatus())
                && Objects.nonNull(kbvItem.getQuestionAnswerResultSummary())
                && kbvItem.getQuestionAnswerResultSummary().getAnsweredIncorrect() > 1;
    }

    private void logVcScore(String result) {
        eventProbe.addDimensions(Map.of(METRIC_DIMENSION_KBV_VERIFICATION, result));
        LOGGER.info("kbv {}", result);
    }
}
