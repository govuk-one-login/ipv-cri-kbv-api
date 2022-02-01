package uk.gov.di.ipv.cri.kbv.api.domain;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ParseJWTTest {
    String jwtString =
            "eyJraWQiOiIxMjMiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvYzJpZC5jb20iLCJzdWIiOiJhbGljZSIsImNsYWltIjp7InZjX2h0dHBfYXBpIjp7ImRhdGVPZkJpcnRoIjoiMTk2NC0wNi0xOCIsInN1cm5hbWUiOiJERUNFUlFVRUlSQSIsImlwdl9zZXNzaW9uX2lkIjoiYWVhNTc3MzUtMTY5OC00M2JhLTgwYmYtZWIyZGFlZDA5NTNmIiwiZmlyc3ROYW1lIjoiS0VOTkVUSCIsImFkZHJlc3NlcyI6W3siYWRkcmVzc1R5cGUiOiJDVVJSRU5UIiwiaG91c2VOdW1iZXIiOiI4IiwidG93bkNpdHkiOiJCQVRIIiwic3RyZWV0IjoiSEFETEVZIFJPQUQiLCJwb3N0Y29kZSI6IkJBMiA1QUEifV19fSwiaWF0IjoxNjQzMjA4NzYzfQ.ZE6E1rJeMjXnbb9HEPVeCzdFeeLRGpffpP709I2cke5vSiaUH4K9LCyAZ4WuBlK4a0D-LNKURirtr6cQov3mMWdB8cpE5U2KNJKZJGkJwgx0ZNHCI4dSQMXocDNCBoOZNl6UKbIpUOd49ydlvcWBSaiRzaxrcLw6KbLWgztm4HDFwR43rC6lUQPYtzOEqBTCbsbv9AKlsnDX-NYJqUfNMdttIKRWBOExn3PJ140ioT4lvp__fGhiIrq32Jh1CpXGUV-WA3jYCJQWyxYW2ElEcK4b-yaVqADvvko0DsI-J4AicEql2F0XCYNLTVJiG5k7gX4b3oWI95gP6Y8JmYfkMw";
    SignedJWT jwt;
    private ParseJWT parseJWT;

    @BeforeEach
    void setUp() throws ParseException {
        parseJWT = new ParseJWT();
        jwt = SignedJWT.parse(jwtString);
    }

    @Test
    void name() throws JOSEException, ParseException {
        /**
         * { "claims": { "userinfo": { "given_name": {"essential": true}, "nickname": null, "email":
         * {"essential": true}, "email_verified": {"essential": true}, "picture": null },
         * "id_token": { "gender": null, "birthdate": {"essential": true}, "acr": {"values":
         * ["urn:mace:incommon:iap:silver"]} } } }
         */
        //        Generate sender RSA key pair, make public key available to recipient:
        RSAKey senderJWK = generateSenderRsaKeyPair();

        RSAKey senderPublicJWK = senderJWK.toPublicJWK();

        //        Generate recipient RSA key pair, make public key available to sender:
        RSAKey recipientJWK = generateRecipientRsaKeyPair();
        RSAKey recipientPublicJWK = recipientJWK.toPublicJWK();
        //         The sender signs the JWT with their private key and then encrypts to the
        // recipient:
        /**
         * { "firstName": "KENNETH", "surname": "DECERQUEIRA", "title": "MR", "dateOfBirth":
         * "1964-06-18", "addresses": [ { "houseNumber": 8, "street": "HADLEY ROAD", "townCity":
         * "BATH", "postcode": "BA2 5AA", "addressType": "CURRENT" } ] }
         */
        // Create JWT
        SignedJWT signedJWT = getSignedJWT(senderJWK);
        // Sign the JWT
        signedJWT.sign(new RSASSASigner(senderJWK));
        String serialize = signedJWT.serialize();
        System.out.println(serialize);
    }

    @Test
    void canParseJwt() throws ParseException, JsonProcessingException {
        assertEquals("{kid=123, alg=RS256}", jwt.getHeader().toJSONObject().toString());
        assertEquals(
                "{\"iss\":\"https:\\/\\/c2id.com\",\"sub\":\"alice\",\"claim\":{\"vc_http_api\":{\"dateOfBirth\":\"1964-06-18\",\"surname\":\"DECERQUEIRA\",\"ipv_session_id\":\"aea57735-1698-43ba-80bf-eb2daed0953f\",\"firstName\":\"KENNETH\",\"addresses\":[{\"addressType\":\"CURRENT\",\"houseNumber\":\"8\",\"townCity\":\"BATH\",\"street\":\"HADLEY ROAD\",\"postcode\":\"BA2 5AA\"}]}},\"iat\":1643208763}",
                jwt.getPayload().toString());
        System.out.println(jwt.getJWTClaimsSet().getClaim("claim"));
        String payload = jwt.getJWTClaimsSet().getClaim("claim").toString();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        JsonNode jsonNode = objectMapper.readTree(payload);
        System.out.println(jsonNode.get("vc_http_api").toString());
        String vcHttp = jsonNode.get("vc_http_api").toString();
        PersonIdentity personIdentity = objectMapper.readValue(vcHttp, PersonIdentity.class);
        // String personIdentityStr = jsonObject.get("vc_http_api").toString();
        assertEquals("", personIdentity);
    }

    // @Test
    void givenARequestHeaderItCanRetrieveAPersonIdentity() throws ParseException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("jwt", jwtString));
        Map<String, String> jwtHeader = request.getHeaders();
        assertEquals(
                parseJWT.parseHeaderJWT(jwtHeader),
                jwt.getJWTClaimsSet().getClaim("claim").toString());
    }

    private RSAKey generateRecipientRsaKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048).keyID("456").keyUse(KeyUse.ENCRYPTION).generate();
    }

    private RSAKey generateSenderRsaKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048).keyID("123").keyUse(KeyUse.SIGNATURE).generate();
    }

    private SignedJWT getSignedJWT(RSAKey senderJWK) {
        return new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(senderJWK.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject("alice")
                        .issueTime(new Date())
                        .issuer("https://c2id.com")
                        .claim(
                                "vc_http_api",
                                Map.of(
                                        "firstName", "KENNETH",
                                        "surname", "DECERQUEIRA",
                                        "dateOfBirth", "1964-06-18",
                                        "addresses",
                                                List.of(
                                                        Map.of(
                                                                "houseNumber",
                                                                "8",
                                                                "street",
                                                                "HADLEY ROAD",
                                                                "townCity",
                                                                "BATH",
                                                                "postcode",
                                                                "BA2 5AA",
                                                                "addressType",
                                                                "CURRENT"))))
                        .claim(
                                "vc_http_session",
                                Map.of("ipv_session_id", UUID.randomUUID().toString()))
                        .claim(
                                "vc_http_strategy",
                                Map.of("ipv_session_id", UUID.randomUUID().toString()))
                        .build());
    }

    void getPersonIdentity() {}

    void getSessionId() {}

    void getStrategy() {}
}
