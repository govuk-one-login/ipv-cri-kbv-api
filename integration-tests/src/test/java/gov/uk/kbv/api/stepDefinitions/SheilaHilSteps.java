package gov.uk.kbv.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SheilaHilSteps {
    public static final String KBV_CRI_DEV = "kbv-cri-dev";
    private static String SESSION_REQUEST_BODY;
    private static String SESSION_ID;
    private final ObjectMapper objectMapper = new ObjectMapper();
    int firstGETStatusCode;
    String firstQuestion;
    String firstQuestionID;
    String secondQuestion;
    String secondQuestionID;
    String thirdQuestion;
    String thirdQuestionID;
    String authorizationResponse;
    String authorizationCode;
    String JWT_STRING;
    int firstQuestionPOSTStatusCode;
    int secondQuestionStatusCode;
    int secondQuestionPOSTResponseCode;
    int thirdQuestionStatusCode;
    int thirdQuestionPOSTResponseCode;
    int fourthQuestionStatusCode;
    int authorizationStatusCode;
    private final int SheilaHilExperianRowNumber = 681;

    private String accessToken;

    private int tokenStatusCode;
    private HttpResponse<String> response;

    private String getPrivateAPIEndpoint() {
        String privateAPIEndpoint = System.getenv("apiGatewayIdPrivate");
        if (privateAPIEndpoint == null) {
            throw new IllegalArgumentException(
                    "Environment variable PRIVATE API endpoint is not set");
        }
        System.out.println("privateAPIEndpoint =>" + privateAPIEndpoint);
        return "https://" + privateAPIEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    private String getPublicAPIEndpoint() {
        String publicAPIEndpoint = System.getenv("apiGatewayIdPublic");
        if (publicAPIEndpoint == null) {
            throw new IllegalArgumentException(
                    "Environment variable PUBLIC API endpoint is not set");
        }
        System.out.println("publicAPIEndpoint =>" + publicAPIEndpoint);
        return "https://" + publicAPIEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    @Given("user has the user identity in the form of a signed JWT string")
    public void user_has_the_user_identity_in_the_form_of_a_signed_jwt_string()
            throws URISyntaxException, IOException, InterruptedException {

        String ipvCoreStubUrl = getIPVCoreStubURL();

        String jsonString =
                getClaimsForUser(ipvCoreStubUrl, KBV_CRI_DEV, SheilaHilExperianRowNumber);
        SESSION_REQUEST_BODY = createRequest(ipvCoreStubUrl, KBV_CRI_DEV, jsonString);
        JsonNode jsonNode = objectMapper.readTree(SESSION_REQUEST_BODY);
        JWT_STRING = jsonNode.get("request").textValue().trim();
        System.out.println("SESSION_REQUEST_BODY = " + SESSION_REQUEST_BODY);
    }

    private String getIPVCoreStubURL() {
        String ipvCoreStubUrl = System.getenv("IPV_CORE_STUB_URL");
        System.out.println("STUB URL =>" + ipvCoreStubUrl);
        if (ipvCoreStubUrl == null) {
            throw new IllegalArgumentException("Environment variable IPV_CORE_STUB_URL is not set");
        }
        return ipvCoreStubUrl;
    }

    private String getClaimsForUser(String baseUrl, String criId, int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {

        var url =
                new URIBuilder(baseUrl)
                        .setPath("backend/generateInitialClaimsSet")
                        .addParameter("cri", criId)
                        .addParameter("rowNumber", String.valueOf(userDataRowNumber))
                        .build();

        System.out.println("URL =>> " + url);

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        var response = sendHttpRequest(request);

        System.out.println("Response Status code:" + response.statusCode());
        System.out.println("Response body:" + response.body());

        return sendHttpRequest(request).body();
    }

    private String getPrivateKeyJWTFormParamsForAuthCode(
            String baseUrl, String criId, String authorizationCode)
            throws URISyntaxException, IOException, InterruptedException {

        var url =
                new URIBuilder(baseUrl)
                        .setPath("backend/createTokenRequestPrivateKeyJWT")
                        .addParameter("cri", criId)
                        .addParameter("authorization_code", authorizationCode)
                        .build();

        System.out.println("URL =>> " + url);

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        var response = sendHttpRequest(request);

        System.out.println("Response Status code:" + response.statusCode());
        String body = response.body();
        System.out.println("Response body:" + body);
        return body;
    }

    private String createRequest(String baseUrl, String criId, String jsonString)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(baseUrl)
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", criId)
                        .build();

        System.out.println("jsonString = " + jsonString);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                        .build();

        return sendHttpRequest(request).body();
    }

    @When("user sends a POST request to session end point")
    public void user_sends_a_post_request_to_session_end_point()
            throws IOException, InterruptedException, URISyntaxException {
        // Write code here that turns the phrase above into concrete actions
        System.out.println("getPrivateAPIEndpoint() ==> " + getPrivateAPIEndpoint());

        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/session").build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("X-Forwarded-For", "192.168.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(SESSION_REQUEST_BODY))
                        .build();
        String sessionResponse = sendHttpRequest(request).body();
        System.out.println("sessionResponse = " + sessionResponse);
        Map<String, String> deserialisedResponse =
                objectMapper.readValue(sessionResponse, new TypeReference<>() {});
        SESSION_ID = deserialisedResponse.get("session_id");
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        System.out.println("SESSION_ID = " + SESSION_ID);
        assertTrue(StringUtils.isNotBlank(SESSION_ID));
    }

    @When("user sends a GET request to question end point")
    public void user_sends_a_get_request_to_question_end_point()
            throws IOException, InterruptedException, URISyntaxException {
        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/question").build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .GET()
                        .build();

        var response = sendHttpRequest(request);
        firstGETStatusCode = response.statusCode();
        Map<String, String> deserialisedGETResponse =
                objectMapper.readValue(response.body(), new TypeReference<>() {});
        firstQuestion = deserialisedGETResponse.get("text");
        firstQuestionID = deserialisedGETResponse.get("questionID");
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {

        String basicAuthUser =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_USER is not set");
        String basicAuthPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_PASSWORD is not set");

        HttpClient client =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                basicAuthUser, basicAuthPassword.toCharArray());
                                    }
                                })
                        .build();

        this.response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    public HttpResponse<String> getResponse() {
        return this.response;
    }

    @Then("user gets the first question with status code {int}")
    public void user_gets_the_first_question_with_status_code(Integer int1) {
        System.out.println("firstQuestion = " + firstQuestion);
        System.out.println("firstGETStatusCode = " + firstGETStatusCode);
        assertEquals(200, firstGETStatusCode);
    }

    @When("user answers the first question correctly")
    public void user_answers_the_first_question_correctly()
            throws IOException, InterruptedException, URISyntaxException {
        System.out.println("firstQuestionID = " + firstQuestionID);
        System.out.println("SESSION_ID = " + SESSION_ID);
        firstQuestionPOSTStatusCode = answerCorrectly(firstQuestionID);
    }

    @Then("user gets status code {int}")
    public void user_gets_status_code(Integer int1) {
        System.out.println("First question POST statusCode() = " + firstQuestionPOSTStatusCode);
        assertEquals(200, firstQuestionPOSTStatusCode);
    }

    @When("user sends a GET request to question end point for the second question")
    public void user_sends_a_get_request_to_question_end_point_for_the_second_question()
            throws IOException, InterruptedException, URISyntaxException {
        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/question").build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .GET()
                        .build();

        var response = sendHttpRequest(request);
        secondQuestionStatusCode = response.statusCode();
        Map<String, String> deserialisedGETResponse =
                objectMapper.readValue(response.body(), new TypeReference<>() {});
        secondQuestion = deserialisedGETResponse.get("text");
        secondQuestionID = deserialisedGETResponse.get("questionID");
    }

    @Then("user gets the second question with status code {int}")
    public void user_gets_the_second_question_with_status_code(Integer int1) {
        System.out.println("secondQuestion = " + secondQuestion);
        System.out.println("secondQuestionStatusCode = " + secondQuestionStatusCode);
        assertEquals(200, secondQuestionStatusCode);
    }

    @When("user answers the second question correctly")
    public void user_answers_the_second_question_correctly()
            throws IOException, InterruptedException, URISyntaxException {
        secondQuestionPOSTResponseCode = answerCorrectly(secondQuestionID);
    }

    @Then("user gets status code {int} for question two POST")
    public void user_gets_status_code_for_question_two_post(Integer int1) {
        System.out.println("secondQuestionPOSTResponseCode = " + secondQuestionPOSTResponseCode);
        assertEquals(200, secondQuestionStatusCode);
    }

    @When("user sends a GET request to question end point for the third question")
    public void user_sends_a_get_request_to_question_end_point_for_the_third_question()
            throws IOException, InterruptedException, URISyntaxException {

        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/question").build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .GET()
                        .build();

        var response = sendHttpRequest(request);
        thirdQuestionStatusCode = response.statusCode();
        if (thirdQuestionStatusCode == 200) {
            Map<String, String> deserialisedGETResponse =
                    objectMapper.readValue(response.body(), new TypeReference<>() {});
            thirdQuestion = deserialisedGETResponse.get("text");
            thirdQuestionID = deserialisedGETResponse.get("questionID");
        }
    }

    @Then("user get the third question with status code {int}")
    public void user_get_the_third_question_with_status_code(Integer int1) {
        System.out.println("thirdQuestion = " + thirdQuestion);
        System.out.println("thirdQuestionStatusCode = " + thirdQuestionStatusCode);
        assertEquals(200, thirdQuestionStatusCode);
    }

    @When("user answers the third question incorrectly")
    public void user_answers_the_third_question_incorrectly()
            throws IOException, InterruptedException {
        thirdQuestionPOSTResponseCode = answerIncorrectly(thirdQuestionID);
    }

    @Then("user gets status code {int} for question three POST")
    public void user_gets_status_code_for_question_three_post(Integer int1) {
        System.out.println("thirdQuestionPOSTResponseCode = " + thirdQuestionPOSTResponseCode);
        assertEquals(200, thirdQuestionPOSTResponseCode);
    }

    @When("user sends a GET request to question end point for the fourth question")
    public void user_sends_a_get_request_to_question_end_point_for_the_fourth_question()
            throws IOException, InterruptedException, URISyntaxException {
        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/question").build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .GET()
                        .build();

        var response = sendHttpRequest(request);
        fourthQuestionStatusCode = response.statusCode();
    }

    @Then("user gets status code {int} for the fourth call")
    public void user_gets_status_code_for_the_fourth_call(Integer int1) {
        System.out.println("fourthQuestionStatusCode = " + fourthQuestionStatusCode);
        assertEquals(204, fourthQuestionStatusCode);
    }

    @When("user answers the second question incorrectly")
    public void user_answers_the_second_question_incorrectly()
            throws IOException, InterruptedException {
        secondQuestionPOSTResponseCode = answerIncorrectly(secondQuestionID);
    }

    @Then("user gets status code {int} for the third call")
    public void user_gets_status_code_for_the_third_call(Integer int1) {
        System.out.println("thirdQuestionStatusCode = " + thirdQuestionStatusCode);
    }

    @When("user sends a GET request to authorization end point")
    public void user_sends_a_get_request_to_authorization_end_point()
            throws IOException, InterruptedException, URISyntaxException {

        String baseUrl = getPrivateAPIEndpoint();
        System.out.println("baseUrl = " + baseUrl);
        var url =
                new URIBuilder(baseUrl)
                        .setPath("/dev/authorization")
                        .addParameter(
                                "redirect_uri",
                                "https://di-ipv-core-stub.london.cloudapps.digital/callback")
                        .addParameter("client_id", "ipv-core-stub")
                        .addParameter("response_type", "code")
                        .addParameter("scope", "openid")
                        .addParameter("state", "state-ipv")
                        .build();

        System.out.println("url in AUTHORIZATION ENDPOINT = " + url);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .GET()
                        .build();

        var response = sendHttpRequest(request);
        authorizationStatusCode = response.statusCode();
        authorizationResponse = response.body();
        System.out.println("authorizationStatusCode = " + authorizationStatusCode);
        System.out.println("authorizationResponse = " + authorizationResponse);
    }

    @Then("user gets status code {int} with authorization code")
    public void user_gets_status_code_with_authorization_code(Integer int1) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(authorizationResponse);
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertEquals(200, authorizationStatusCode);
        assertNotNull(authorizationCode);
    }

    @When("user sends a POST request to token end point")
    public void user_sends_a_post_request_to_token_end_point()
            throws URISyntaxException, IOException, InterruptedException {

        String baseUrl = getPublicAPIEndpoint();

        var url = new URIBuilder(baseUrl).setPath("/dev/token").build();

        System.out.println("authorizationCode = " + authorizationCode);

        System.out.println("JWT_STRING in AUTHORIZATION ENDPOINT = " + JWT_STRING);
        String privateKeyJWT =
                getPrivateKeyJWTFormParamsForAuthCode(
                        getIPVCoreStubURL(), KBV_CRI_DEV, authorizationCode.trim());
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("x-api-key", getPublicAPIKey())
                        .POST(HttpRequest.BodyPublishers.ofString(privateKeyJWT))
                        .build();

        var response = sendHttpRequest(request);
        System.out.println("response status code = " + response.statusCode());
        System.out.println("response body = " + response.body());
    }

    @Then("user gets status code {int} with a valid access token code")
    public void user_gets_status_code_with_a_valid_access_token_code(Integer int1)
            throws IOException {
        JsonNode jsonNode = objectMapper.readTree(getResponse().body());
        var accessToken = jsonNode.get("access_token").asText();
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();

        assertEquals(200, getResponse().statusCode());
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        assertFalse(accessToken.isEmpty());
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
            throws IOException, InterruptedException, URISyntaxException {
        JsonNode jsonNode = objectMapper.readTree(getResponse().body());
        var accessToken = jsonNode.get("access_token").asText();
        System.out.println("accessToken = " + accessToken);

        var url = new URIBuilder(getPublicAPIEndpoint()).setPath("/dev/credential/issue").build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .header("x-api-key", getPublicAPIKey())
                        .setHeader("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

        sendHttpRequest(request);
    }

    @Then("user gets status code {int} and a JWT")
    public void user_gets_status_code_and_a_jwt(Integer int1) {
        System.out.println("getResponse().body() = " + getResponse().body());
        assertNotNull(getResponse().body());
        assertEquals(200, getResponse().statusCode());
    }

    private String getPublicAPIKey() {
        return Optional.ofNullable(System.getenv("APIGW_API_KEY")).orElseThrow();
    }

    private HttpRequest.BodyPublisher getParamsUrlEncoded(Map<String, String> parameters) {
        String urlEncoded =
                parameters.entrySet().stream()
                        .map(
                                e ->
                                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                                + "="
                                                + URLEncoder.encode(
                                                        e.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));
        return HttpRequest.BodyPublishers.ofString(urlEncoded);
    }

    private int answerIncorrectly(String question) throws IOException, InterruptedException {
        String answer = null;
        if (question.equalsIgnoreCase("Q00001")) {
            answer = "Incorrect1";
        } else if (question.equalsIgnoreCase("Q00002")) {
            answer = "Incorrect2";
        }

        System.out.println("answer = " + answer);
        String POST_REQUEST_BODY =
                "{\"questionId\":\"" + question + "\",\"answer\":\"" + answer + "\"}";
        System.out.println("POST_REQUEST_BODY = " + POST_REQUEST_BODY);
        var body = HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(getPrivateAPIEndpoint() + "/answer"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .POST(body)
                        .build();
        var response = sendHttpRequest(request);
        return response.statusCode();
    }

    private int answerCorrectly(String question)
            throws IOException, InterruptedException, URISyntaxException {
        String answer = null;
        if (question.equalsIgnoreCase("Q00001")) {
            answer = "Correct 1";
        } else if (question.equalsIgnoreCase("Q00002")) {
            answer = "Correct 2";
        }

        System.out.println("answer = " + answer);
        String POST_REQUEST_BODY =
                "{\"questionId\":\"" + question + "\",\"answer\":\"" + answer + "\"}";
        System.out.println("POST_REQUEST_BODY = " + POST_REQUEST_BODY);
        var body = HttpRequest.BodyPublishers.ofString(POST_REQUEST_BODY);

        var url = new URIBuilder(getPrivateAPIEndpoint()).setPath("/dev/answer").build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .setHeader("session-id", SESSION_ID)
                        .POST(body)
                        .build();
        var response = sendHttpRequest(request);
        return response.statusCode();
    }
}
