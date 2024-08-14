package uk.gov.di.ipv.cri.kbv.api.objectmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JWSHeaderSerializerTest {
    @Test
    void shouldSerializeJWSHeadersInOrder() throws JsonProcessingException {
        JWSHeader header =
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .type(JOSEObjectType.JWT)
                        .keyID("did:web:issuer:dummyissuer")
                        .build();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(JWSHeader.class, new JWSHeaderSerializer());
        mapper.registerModule(module);

        String jsonOutput = mapper.writeValueAsString(header);

        String expectedJson =
                "{\"typ\":\"JWT\",\"alg\":\"ES256\",\"kid\":\"did:web:issuer:dummyissuer\"}";

        assertNotEquals(expectedJson, header.toString());
        assertEquals(expectedJson, jsonOutput);
    }

    @Test
    void shouldSerializeJWSHeadersStringInputInOrder()
            throws JsonProcessingException, ParseException {
        JWSHeader header =
                JWSHeader.parse(
                        "{\"typ\":\"JWT\",\"alg\":\"ES256\",\"kid\":\"did:web:issuer:dummyissuer\"}");

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(JWSHeader.class, new JWSHeaderSerializer());
        mapper.registerModule(module);

        String jsonOutput = mapper.writeValueAsString(header);

        String expectedJson =
                "{\"typ\":\"JWT\",\"alg\":\"ES256\",\"kid\":\"did:web:issuer:dummyissuer\"}";

        assertNotEquals(expectedJson, header.toString());
        assertEquals(expectedJson, jsonOutput);
    }

    @Test
    void shouldSerializerJWSHeaderWithNullValues() throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).type(null).keyID(null).build();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(JWSHeader.class, new JWSHeaderSerializer());
        mapper.registerModule(module);

        String jsonOutput = mapper.writeValueAsString(header);

        String expectedJson = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
        assertEquals(expectedJson, jsonOutput);
    }
}
