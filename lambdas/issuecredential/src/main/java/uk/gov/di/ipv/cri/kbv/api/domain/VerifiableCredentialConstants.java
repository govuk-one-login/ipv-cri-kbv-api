package uk.gov.di.ipv.cri.kbv.api.domain;

public class VerifiableCredentialConstants {
    public static final String KBV_CREDENTIAL_TYPE = "IdentityCheckCredential";

    public static final String VC_ADDRESS_KEY = "address";
    public static final String VC_BIRTHDATE_KEY = "birthDate";
    public static final String VC_NAME_KEY = "name";

    public static final String VC_EVIDENCE_KEY = "evidence";
    public static final String VC_EVIDENCE_TYPE = "IdentityCheck";

    public static final String VC_THIRD_PARTY_KBV_CHECK_PASS = "AUTHENTICATED";
    public static final String VC_THIRD_PARTY_KBV_CHECK_NOT_AUTHENTICATED = "NOT AUTHENTICATED";
    public static final String VC_THIRD_PARTY_KBV_CHECK_UNABLE_TO_AUTHENTICATE =
            "UNABLE TO AUTHENTICATE";
    public static final String VC_THIRD_PARTY_KBV_CHECK_ABANDONED = "ABANDONED";

    public static final int VC_PASS_EVIDENCE_SCORE = 2;
    public static final int VC_FAIL_EVIDENCE_SCORE = 0;

    private VerifiableCredentialConstants() {}
}
