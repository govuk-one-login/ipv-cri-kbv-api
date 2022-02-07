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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.APERSONIDENTITY;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.BADJWT;
import static uk.gov.di.ipv.cri.kbv.api.data.TestData.GOODJWT;

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
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(Map.of("request", GOODJWT));

        Optional<PersonIdentity> personIdentity = parseJWT.getPersonIdentity(event);

        PersonIdentity person = objectMapper.readValue(APERSONIDENTITY, PersonIdentity.class);

        assertFalse(personIdentity.isEmpty());
        personIdentity.ifPresent(
                p -> {
                    assertTrue(p.getFirstName().equals(person.getFirstName()));
                    assertTrue(p.getSurname().equals(person.getSurname()));
                    assertTrue(p.getDateOfBirth().isEqual(person.getDateOfBirth()));
                    assertFalse(p.getAddresses().isEmpty());
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getHouseNumber()
                                    .equals(person.getAddresses().get(0).getHouseNumber()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getStreet()
                                    .equals(person.getAddresses().get(0).getStreet()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getTownCity()
                                    .equals(person.getAddresses().get(0).getTownCity()));
                    assertTrue(
                            p.getAddresses()
                                    .get(0)
                                    .getPostcode()
                                    .equals(person.getAddresses().get(0).getPostcode()));
                    assertTrue(
                            p.getAddresses().get(0).getAddressType().equals(AddressType.CURRENT));
                });
    }

    @Test
    public void shouldThrowParseExceptionForIncorrectJWT() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(Map.of("request", "incorrect-jwt"));
        Exception exception =
                assertThrows(
                        ParseException.class,
                        () -> {
                            parseJWT.getPersonIdentity(event);
                        });
        String expectedMessage =
                "Invalid serialized unsecured/JWS/JWE object: Missing part delimiters";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void shouldThrowJsonProcessingException() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(Map.of("request", BADJWT));
        assertThrows(
                JsonProcessingException.class,
                () -> {
                    parseJWT.getPersonIdentity(event);
                });
    }
}
