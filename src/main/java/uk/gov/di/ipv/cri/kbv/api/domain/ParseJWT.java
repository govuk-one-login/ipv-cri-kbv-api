package uk.gov.di.ipv.cri.kbv.api.domain;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.util.Optional;

public class ParseJWT {

    private final ObjectMapper objectMapper;

    public ParseJWT(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<PersonIdentity> getPersonIdentity(APIGatewayProxyRequestEvent input)
            throws ParseException, JsonProcessingException {
        try {
            String jwt = input.getQueryStringParameters().get("request");
            SignedJWT jwtString = SignedJWT.parse(jwt);
            String payload = jwtString.getJWTClaimsSet().getClaim("claim").toString();
            JsonNode node = objectMapper.readTree(payload).get("vc_http_api");
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove("ipv_session_id");
            return Optional.ofNullable(
                    objectMapper.readValue(node.toString(), PersonIdentity.class));
        } catch (ParseException | JsonProcessingException e) {
            throw e;
        }
    }
}
