package uk.gov.di.ipv.cri.kbv.api.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JWTClaimsSetSerializer extends JsonSerializer<JWTClaimsSet> {
    private static final String BIRTH_DATE = "birthDate";
    private static final String NAME = "name";
    private static final String ADDRESS = "address";
    private static final String CONTEXT = "@context";
    private static final String CREDENTIAL_SUBJECT = "credentialSubject";
    public static final String EVIDENCE = "evidence";

    @Override
    public void serialize(JWTClaimsSet claimsSet, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();

        if (claimsSet.getIssuer() != null) {
            gen.writeStringField("iss", claimsSet.getIssuer());
        }
        if (claimsSet.getSubject() != null) {
            gen.writeStringField("sub", claimsSet.getSubject());
        }
        if (claimsSet.getNotBeforeTime() != null) {
            gen.writeNumberField("nbf", claimsSet.getNotBeforeTime().getTime() / 1000);
        }
        if (claimsSet.getExpirationTime() != null) {
            gen.writeNumberField("exp", claimsSet.getExpirationTime().getTime() / 1000);
        }

        serializeVcClaim(claimsSet.getClaim("vc"), gen);

        if (claimsSet.getJWTID() != null) {
            gen.writeStringField("jti", claimsSet.getJWTID());
        }

        gen.writeEndObject();
    }

    private void serializeVcClaim(Object vcClaim, JsonGenerator gen) throws IOException {
        if (vcClaim instanceof Map) {
            Map<String, Object> vc = (Map<String, Object>) vcClaim;
            gen.writeObjectFieldStart("vc");

            if (vc.containsKey(CONTEXT)) {
                gen.writeObjectField(CONTEXT, vc.get(CONTEXT));
            }
            for (Map.Entry<String, Object> entry : vc.entrySet()) {
                if (CREDENTIAL_SUBJECT.equals(entry.getKey()) && entry.getValue() instanceof Map) {
                    serializeCredentialSubject((Map<String, Object>) entry.getValue(), gen);
                } else if (EVIDENCE.equals(entry.getKey())) {
                    if (entry.getValue() instanceof List) {
                        serializeEvidence((List<?>) entry.getValue(), gen);
                    }
                } else {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
            }
            gen.writeEndObject();
        }
    }

    private void serializeCredentialSubject(
            Map<String, Object> credentialSubject, JsonGenerator gen) throws IOException {
        gen.writeObjectFieldStart(CREDENTIAL_SUBJECT);

        // Order the fields as name, address, birthdate
        if (credentialSubject.containsKey(NAME)) {
            gen.writeObjectField(NAME, credentialSubject.get(NAME));
        }
        if (credentialSubject.containsKey(ADDRESS)) {
            gen.writeObjectField(ADDRESS, credentialSubject.get(ADDRESS));
        }
        if (credentialSubject.containsKey(BIRTH_DATE)) {
            gen.writeObjectField(BIRTH_DATE, credentialSubject.get(BIRTH_DATE));
        }

        gen.writeEndObject();
    }

    private void serializeEvidence(List<?> evidenceList, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(EVIDENCE);
        for (Object evidenceItem : evidenceList) {
            gen.writeObject(evidenceItem);
        }
        gen.writeEndArray();
    }
}
