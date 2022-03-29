package uk.gov.di.ipv.cri.kbv.api.library.domain;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.cri.kbv.api.library.data.TestData.BADJWT;
import static uk.gov.di.ipv.cri.kbv.api.library.data.TestData.GOODJWT;
import static uk.gov.di.ipv.cri.kbv.api.library.data.TestData.PERSON_SHARED_ATTRIBUTE;

@ExtendWith(MockitoExtension.class)
class ParseJWTTest {

    private ObjectMapper objectMapper;
    private ParseJWT parseJWT;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        parseJWT = new ParseJWT(objectMapper);
    }

    @Test
    void shouldReturnPersonIdentityForAValidJWT() throws ParseException, JsonProcessingException {

        Optional<PersonIdentity> personIdentity = parseJWT.getPersonIdentity(GOODJWT);

        PersonIdentitySharedAttribute person =
                objectMapper.readValue(
                        PERSON_SHARED_ATTRIBUTE, PersonIdentitySharedAttribute.class);

        assertFalse(personIdentity.isEmpty());
        personIdentity.ifPresent(
                p -> {
                    assertEquals(p.getFirstName(), person.getNames().get(0).getFirstName());
                    assertEquals(p.getSurname(), person.getNames().get(0).getSurname());
                    assertTrue(
                            p.getDateOfBirth()
                                    .isEqual(LocalDate.parse(person.getDatesOfBirth().get(0))));
                    assertFalse(p.getAddresses().isEmpty());
                    assertEquals(
                            p.getAddresses().get(0).getHouseNumber(),
                            person.getUkAddresses().get(0).getStreet1());
                    assertEquals(
                            p.getAddresses().get(0).getStreet(),
                            person.getUkAddresses().get(0).getStreet2());
                    assertEquals(
                            p.getAddresses().get(0).getTownCity(),
                            person.getUkAddresses().get(0).getTownCity());
                    assertEquals(
                            p.getAddresses().get(0).getPostcode(),
                            person.getUkAddresses().get(0).getPostCode());
                    assertEquals(AddressType.CURRENT, p.getAddresses().get(0).getAddressType());
                });
    }

    @Test
    void shouldThrowParseExceptionForIncorrectJWT() {
        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent();
        apiGatewayProxyRequestEvent.setBody("{request:incorrect-jwt}");
        Exception exception =
                assertThrows(
                        ParseException.class,
                        () -> {
                            parseJWT.getPersonIdentity("incorrect-jwt");
                        });
        String expectedMessage =
                "Invalid serialized unsecured/JWS/JWE object: Missing part delimiters";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void shouldThrowJsonProcessingException() {
        Exception exception =
                assertThrows(
                        JsonProcessingException.class,
                        () -> {
                            parseJWT.getPersonIdentity(BADJWT);
                        });
        String expectedMessage = "Unrecognized field \"firstName1234\"";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
