package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class ExperianServiceTest {

    private static final String RTQ = "/question-answer";
    private static final String SAA = "/question-request";
    private ExperianService experianService;
    private String experianURL = "http://localhost:8080";
    public static final String EXPECTED_QUESTION =
            "{\"questionID\":\"Q00015\",\"text\":\"What is the first James Bond movie?\",\"tooltip\":\"It was released in the 1960s.\",\"answerHeldFlag\":null,\"answerFormat\":{\"identifier\":\"A00004\",\"fieldType\":\"G \",\"answerList\":[\"Movie 1\",\"Movie 2\",\"Movie 3\"]}}";

    @SystemStub private EnvironmentVariables environmentVariables;

    @Spy HttpClient httpClient;

    @BeforeEach
    void setUp() {
        experianService = new ExperianService(httpClient);
        environmentVariables.set("EXPERIAN_API_WRAPPER_URL", experianURL);
    }

    @Test
    void shouldReturnResponseFromKBVExperianAPIForQuestionAnswerEndpoint()
            throws IOException, InterruptedException {
        environmentVariables.set("EXPERIAN_API_WRAPPER_RTQ_RESOURCE", RTQ);
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(EXPECTED_QUESTION);
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);

        String experianAPIResponse =
                experianService.getResponseFromKBVExperianAPI(
                        "request-payload", "EXPERIAN_API_WRAPPER_RTQ_RESOURCE");
        assertNotNull(experianAPIResponse);
        assertTrue(experianAPIResponse.contains(EXPECTED_QUESTION));
    }

    @Test
    void shouldReturnResponseFromKBVExperianAPIForQuestionRequestEndpoint()
            throws IOException, InterruptedException {
        environmentVariables.set("EXPERIAN_API_WRAPPER_SAA_RESOURCE", SAA);
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(EXPECTED_QUESTION);
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(mockResponse);

        String experianAPIResponse =
                experianService.getResponseFromKBVExperianAPI(
                        "request-payload", "EXPERIAN_API_WRAPPER_SAA_RESOURCE");
        assertNotNull(experianAPIResponse);
        assertTrue(experianAPIResponse.contains(EXPECTED_QUESTION));
    }
}
