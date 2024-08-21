package uk.gov.di.ipv.cri.kbv.api.objectmapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.kbv.api.objectmapper.mixin.AddressMixIn;

public class CustomObjectMapper {
    @ExcludeFromGeneratedCoverageReport
    private CustomObjectMapper() {
        throw new UnsupportedOperationException("CustomObjectMapper - cannot be instantiated");
    }

    public static ObjectMapper getMapperWithCustomSerializers() {
        ObjectMapper objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        objectMapper.addMixIn(CanonicalAddress.class, AddressMixIn.class);
        objectMapper.addMixIn(Address.class, AddressMixIn.class);

        SimpleModule module = new SimpleModule();
        module.addSerializer(JWTClaimsSet.class, new JWTClaimsSetSerializer());
        module.addSerializer(JWSHeader.class, new JWSHeaderSerializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }
}