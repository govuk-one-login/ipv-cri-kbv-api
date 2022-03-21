package uk.gov.di.ipv.cri.kbv.api.domain;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.BADJWT;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.GOODJWT;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.PERSON_SHARED_ATTRIBUTE;

@ExtendWith(MockitoExtension.class)
public class ParseJWTTest {

    private ObjectMapper objectMapper;
    private ParseJWT parseJWT;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        parseJWT = new ParseJWT(objectMapper);
    }

    @Test
    public void shouldReturnPersonIdentityForAValidJWT()
            throws ParseException, JsonProcessingException {

        Optional<PersonIdentity> personIdentity = parseJWT.getPersonIdentity(GOODJWT);

        PersonIdentitySharedAttribute person =
                objectMapper.readValue(
                        PERSON_SHARED_ATTRIBUTE, PersonIdentitySharedAttribute.class);

        assertFalse(personIdentity.isEmpty());
        personIdentity.ifPresent(
                p -> {
                    assertTrue(p.getFirstName().equals(person.getNames().get(0).getFirstName()));
                    assertTrue(p.getSurname().equals(person.getNames().get(0).getSurname()));
                    assertTrue(
                            p.getDateOfBirth()
                                    .isEqual(LocalDate.parse(person.getDatesOfBirth().get(0))));
                    assertFalse(p.getAddresses().isEmpty());
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getHouseNumber()
                                    .equals(person.getUKAddresses().get(0).getStreet1()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getStreet()
                                    .equals(person.getUKAddresses().get(0).getStreet2()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getTownCity()
                                    .equals(person.getUKAddresses().get(0).getTownCity()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getPostcode()
                                    .equals(person.getUKAddresses().get(0).getPostCode()));
                    assertTrue(
                            p.getAddresses().get(0).getAddressType().equals(AddressType.CURRENT));
                });
    }

    @Test
    public void shouldThrowParseExceptionForIncorrectJWT() {
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
    public void shouldThrowJsonProcessingException() {
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
