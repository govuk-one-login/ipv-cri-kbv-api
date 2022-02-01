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

@ExtendWith(MockitoExtension.class)
public class ParseJWTTest {

    private ObjectMapper objectMapper;
    private ParseJWT parseJWT;

    private final String jwt =
            "eyJraWQiOiIxMjMiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvYzJpZC5jb20iLCJzdWIiOiJhbGljZSIsImNsYWltIjp7InZjX2h0dHBfYXBpIjp7ImRhdGVPZkJpcnRoIjoiMTk2NC0wNi0xOCIsInN1cm5hbWUiOiJERUNFUlFVRUlSQSIsImlwdl9zZXNzaW9uX2lkIjoiYWVhNTc3MzUtMTY5OC00M2JhLTgwYmYtZWIyZGFlZDA5NTNmIiwiZmlyc3ROYW1lIjoiS0VOTkVUSCIsImFkZHJlc3NlcyI6W3siYWRkcmVzc1R5cGUiOiJDVVJSRU5UIiwiaG91c2VOdW1iZXIiOiI4IiwidG93bkNpdHkiOiJCQVRIIiwic3RyZWV0IjoiSEFETEVZIFJPQUQiLCJwb3N0Y29kZSI6IkJBMiA1QUEifV19fSwiaWF0IjoxNjQzMjA4NzYzfQ.ZE6E1rJeMjXnbb9HEPVeCzdFeeLRGpffpP709I2cke5vSiaUH4K9LCyAZ4WuBlK4a0D-LNKURirtr6cQov3mMWdB8cpE5U2KNJKZJGkJwgx0ZNHCI4dSQMXocDNCBoOZNl6UKbIpUOd49ydlvcWBSaiRzaxrcLw6KbLWgztm4HDFwR43rC6lUQPYtzOEqBTCbsbv9AKlsnDX-NYJqUfNMdttIKRWBOExn3PJ140ioT4lvp__fGhiIrq32Jh1CpXGUV-WA3jYCJQWyxYW2ElEcK4b-yaVqADvvko0DsI-J4AicEql2F0XCYNLTVJiG5k7gX4b3oWI95gP6Y8JmYfkMw";

    private final String mockPersonIdentity =
            "{\n"
                    + "              \"firstName\": \"KENNETH\",\n"
                    + "              \"surname\": \"DECERQUEIRA\",\n"
                    + "              \"title\": \"MR\",\n"
                    + "              \"dateOfBirth\": \"1964-06-18\",\n"
                    + "              \"addresses\": [\n"
                    + "                  {\n"
                    + "                      \"houseNumber\": 8,\n"
                    + "                      \"street\": \"HADLEY ROAD\",\n"
                    + "                      \"townCity\": \"BATH\",\n"
                    + "                      \"postcode\": \"BA2 5AA\",\n"
                    + "                      \"addressType\": \"CURRENT\"\n"
                    + "                  }\n"
                    + "              ]\n"
                    + "    }";

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
        event.setQueryStringParameters(Map.of("request", jwt));

        Optional<PersonIdentity> personIdentity = parseJWT.getPersonIdentity(event);

        PersonIdentity person = objectMapper.readValue(mockPersonIdentity, PersonIdentity.class);

        assertFalse(personIdentity.isEmpty());
        personIdentity.ifPresent(
                p -> {
                    assertTrue(p.getFirstName().equals(person.getFirstName()));
                    assertTrue(p.getSurname().equals(person.getSurname()));
                    assertTrue(p.getDateOfBirth().isEqual(person.getDateOfBirth()));
                    assertTrue(!p.getAddresses().isEmpty());
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
    public void shouldThrowParseExceptionForIncorrectJWT()
            throws ParseException, JsonProcessingException {
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
    public void shouldThrowJsonProcessingException()
            throws ParseException, JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setQueryStringParameters(
                Map.of(
                        "request",
                        "eyJraWQiOiIxMjMiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvYzJpZC5jb20iLCJzdWIiOiJhbGljZSIsImNsYWltIjp7InZjX2h0dHBfYXBpIjp7ImRhdGVPZkJpcnRoIjoiMTk2NC0wNi0xOCIsInN1cm5hbWUiOiJERUNFUlFVRUlSQSIsImlwdl9zZXNzaW9uX2lkIjoiODJkYmZkMzgtMjU1Zi00ZmE2LTg3OWEtMzY3YmVjODBjMDMyIiwiYWRkcmVzc2VzIjpbeyJhZGRyZXNzVHlwZSI6IkNVUlJFTlQiLCJob3VzZU51bWJlciI6IjgiLCJ0b3duQ2l0eSI6IkJBVEgiLCJzdHJlZXQiOiJIQURMRVkgUk9BRCIsInBvc3Rjb2RlIjoiQkEyIDVBQSJ9XSwiZmlyc3ROYW1lMSI6IktFTk5FVEgifX0sImlhdCI6MTY0MzgxODAzNH0.BoERoaKSvJROtPD0QXNQ9P6pBZFcDN6e-2nxzvFkb9D2W8zoB2ASwqYG99d6syXbk6N8l7peNykQQHXF00HNpG86x1vC74HRmhj7tVhuNHC8dkIqPefk9vb5f32WNszb1W24wxafMkvB9AdswekR27NMyGBB-cSBwQvIXDEh69QHd3zleWovMADT_68ZQEy6PyvDHft8twWtAgDOC7t0PEQuLY8Jz5o5Gi4RxkXHQV5_-z6S6197jRMo-q-cdVc92LFWLvL7FFPvn6XadqWPZM7KEbPN7TqMdGII0UzNU152lejCvGyrfJUCM5AIlUvOLjJiAN6xZOzIaGF0Vbydfw"));
        assertThrows(
                JsonProcessingException.class,
                () -> {
                    parseJWT.getPersonIdentity(event);
                });
    }
}
