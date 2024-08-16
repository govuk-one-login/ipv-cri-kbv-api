package uk.gov.di.ipv.cri.kbv.api.handler.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import org.jetbrains.annotations.NotNull;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class JwtSigner {
    private static final String EC_PRIVATE_KEY_1 =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";

    private JwtSigner() {
        throw new UnsupportedOperationException("JwtSigner - cannot be instantiated");
    }

    @NotNull
    public static ECDSASigner getEcdsaSigner()
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException {
        ECPrivateKey privateKey =
                (ECPrivateKey)
                        KeyFactory.getInstance("EC")
                                .generatePrivate(
                                        new PKCS8EncodedKeySpec(
                                                Base64.getDecoder().decode(EC_PRIVATE_KEY_1)));

        return new ECDSASigner(privateKey);
    }
}
