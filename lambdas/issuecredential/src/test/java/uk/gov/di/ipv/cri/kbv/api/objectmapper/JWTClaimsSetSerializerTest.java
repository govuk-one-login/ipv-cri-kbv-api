package uk.gov.di.ipv.cri.kbv.api.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.kbv.api.domain.Evidence;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JWTClaimsSetSerializerTest {
    private JWTClaimsSetSerializer serializer;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        serializer = new JWTClaimsSetSerializer();
        serializerProvider = mock(SerializerProvider.class);
    }

    @Test
    void shouldOrderClaimsAsExpectedWhenSerialized() throws IOException {
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .notBeforeTime(new Date(4070908800000L))
                        .subject("subject")
                        .expirationTime(new Date(4070909400000L))
                        .jwtID("dummyJti")
                        .issuer("dummyAddressComponentId")
                        .claim("vc", Collections.emptyMap())
                        .build();

        StringWriter writer = new StringWriter();
        JsonGenerator generator = new ObjectMapper().getFactory().createGenerator(writer);

        serializer.serialize(claimsSet, generator, serializerProvider);
        generator.flush();

        String expectedJson =
                "{\"iss\":\"dummyAddressComponentId\",\"sub\":\"subject\",\"nbf\":4070908800,\"exp\":4070909400,\"vc\":{},\"jti\":\"dummyJti\"}";

        assertEquals(expectedJson, writer.toString().trim());
    }

    @Test
    void shouldSerializeWithEvidence() throws IOException {
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        Evidence evidenceOne = new Evidence();
        Evidence evidenceTwo = new Evidence();
        List<Evidence> evidence = List.of(evidenceOne, evidenceTwo);

        Map<String, Object> vcClaim = new HashMap<>();
        vcClaim.put(JWTClaimsSetSerializer.EVIDENCE, evidence);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("vc", vcClaim).build();

        serializer.serialize(claimsSet, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeStartObject();

        verify(jsonGenerator).writeObjectFieldStart("vc");
        verify(jsonGenerator).writeArrayFieldStart(JWTClaimsSetSerializer.EVIDENCE);
        verify(jsonGenerator).writeObject(evidenceOne);
        verify(jsonGenerator).writeObject(evidenceTwo);
        verify(jsonGenerator).writeEndArray();
        verify(jsonGenerator, times(2)).writeEndObject();
    }

    @Test
    void shouldSerializeEmptyClaimsSet() throws IOException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();

        StringWriter writer = new StringWriter();
        JsonGenerator generator = new ObjectMapper().getFactory().createGenerator(writer);

        serializer.serialize(claimsSet, generator, serializerProvider);
        generator.flush();

        String expectedJson = "{}";
        assertEquals(expectedJson, writer.toString());
    }
}
