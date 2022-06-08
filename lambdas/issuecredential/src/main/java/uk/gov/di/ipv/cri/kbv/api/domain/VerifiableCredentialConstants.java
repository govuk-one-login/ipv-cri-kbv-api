package uk.gov.di.ipv.cri.kbv.api.domain;

public class VerifiableCredentialConstants {
    public static final String VC_CONTEXT = "@context";
    public static final String W3_BASE_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    public static final String DI_CONTEXT =
            "https://vocab.london.cloudapps.digital/contexts/identity-v1.jsonld";
    public static final String VC_TYPE = "type";
    public static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";

    public static final String KBV_CREDENTIAL_TYPE = "IdentityCheckCredential";
    public static final String VC_CREDENTIAL_SUBJECT = "credentialSubject";
    public static final String VC_CLAIM = "vc";

    public static final String VC_ADDRESS_KEY = "address";

    public static final String VC_BIRTHDATE_KEY = "birthDate";

    public static final String VC_NAME_KEY = "name";

    public static final String VC_EVIDENCE_KEY = "evidence";

    public static final String VC_EVIDENCE_TYPE = "IdentityCheck";

    public static final String VC_THIRD_PARTY_KBV_CHECK_PASS = "AUTHENTICATED";

    public static final String VC_THIRD_PARTY_KBV_CHECK_FAIL = "NOT AUTHENTICATED";

    public static final int VC_PASS_EVIDENCE_SCORE = 2;

    public static final int VC_FAIL_EVIDENCE_SCORE = 0;

    private VerifiableCredentialConstants() {}
}
