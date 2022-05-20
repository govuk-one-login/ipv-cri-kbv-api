// package uk.gov.di.ipv.cri.kbv.api.domain;
//
// import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;
// import uk.gov.di.ipv.cri.kbv.api.trash.SharedClaims;
//
// import java.text.ParseException;
// import java.time.LocalDate;
// import java.util.Optional;
//
// import static org.junit.jupiter.api.Assertions.*;
// import static uk.gov.di.ipv.cri.kbv.api.data.TestData.*;
//
// @ExtendWith(MockitoExtension.class)
// class ParseJWTTest {
//
//    private ObjectMapper objectMapper;
//    private ParseJWT parseJWT;
//
//    @BeforeEach
//    void setup() {
//        objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        parseJWT = new ParseJWT(objectMapper);
//    }
//
//    @Test
//    void shouldReturnPersonIdentityForAValidJWT() throws ParseException, JsonProcessingException {
//
//        Optional<PersonIdentity> personIdentity = parseJWT.getPersonIdentity(GOODJWT);
//
//        SharedClaims person = objectMapper.readValue(PERSON_SHARED_ATTRIBUTE, SharedClaims.class);
//
//        assertFalse(personIdentity.isEmpty());
//        personIdentity.ifPresent(
//                p -> {
//                    assertEquals(
//                            p.getFirstName(),
// person.getNames().get(0).getNameParts().get(0).getValue());
//                    assertEquals(
//                            p.getSurname(),
// person.getNames().get(0).getNameParts().get(1).getValue());
//                    assertTrue(
//                            p.getDateOfBirth()
//                                    .isEqual(
//                                            LocalDate.parse(
//                                                    person.getBirthDates().get(0).getValue())));
//                    assertFalse(p.getAddresses().isEmpty());
//                    assertEquals(
//                            p.getAddresses().get(0).getHouseNumber(),
//                            person.getAddresses().get(0).getBuildingNumber());
//                    assertEquals(
//                            p.getAddresses().get(0).getStreet(),
//                            person.getAddresses().get(0).getStreetName());
//                    assertEquals(
//                            p.getAddresses().get(0).getTownCity(),
//                            person.getAddresses().get(0).getAddressLocality());
//                    assertEquals(
//                            p.getAddresses().get(0).getPostcode(),
//                            person.getAddresses().get(0).getPostalCode());
//                    assertEquals(AddressType.CURRENT, p.getAddresses().get(0).getAddressType());
//                });
//    }
//
//    @Test
//    void shouldThrowParseExceptionForIncorrectJWT() {
//        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new
// APIGatewayProxyRequestEvent();
//        apiGatewayProxyRequestEvent.setBody("{request:incorrect-jwt}");
//        Exception exception =
//                assertThrows(
//                        ParseException.class,
//                        () -> {
//                            parseJWT.getPersonIdentity("incorrect-jwt");
//                        });
//        String expectedMessage =
//                "Invalid serialized unsecured/JWS/JWE object: Missing part delimiters";
//        String actualMessage = exception.getMessage();
//        assertTrue(actualMessage.contains(expectedMessage));
//    }
//
//    @Test
//    void shouldThrowJsonProcessingException() {
//        Exception exception =
//                assertThrows(
//                        JsonProcessingException.class,
//                        () -> {
//                            parseJWT.getPersonIdentity(BADJWT);
//                        });
//        String expectedMessage = "Unrecognized field \"nameParts1234\"";
//        String actualMessage = exception.getMessage();
//        assertTrue(actualMessage.contains(expectedMessage));
//    }
// }
