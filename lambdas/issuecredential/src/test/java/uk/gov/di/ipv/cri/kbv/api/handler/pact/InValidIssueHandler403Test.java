package uk.gov.di.ipv.cri.kbv.api.handler.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.kbv.api.handler.IssueCredentialHandler;
import uk.gov.di.ipv.cri.kbv.api.handler.pact.states.DummyStates;
import uk.gov.di.ipv.cri.kbv.api.handler.util.Injector;
import uk.gov.di.ipv.cri.kbv.api.handler.util.MockHttpServer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.kbv.api.handler.IssueCredentialHandler.KBV_CREDENTIAL_ISSUER;

@Tag("Pact")
@Provider("ExperianKbvCriVcProvider")
@PactFolder("pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class InValidIssueHandler403Test implements DummyStates {
    @SystemStub EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Mock private SessionService sessionServiceMock;
    @Mock private EventProbe eventProbeMock;
    private static final int PORT = 5020;
    private static final boolean ENABLE_FULL_DEBUG = true;
    @InjectMocks IssueCredentialHandler handler;

    @BeforeAll
    static void setupServer() {
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact_do_not_track", "true");

        System.setProperty(
                "pact.filter.description",
                "Invalid credential request due to invalid access token");
        if (ENABLE_FULL_DEBUG) {
            // AutoConfig SL4j with Log4J
            BasicConfigurator.configure();
            Configurator.setAllLevels("", Level.DEBUG);
        }
    }

    @AfterEach
    public void tearDown() {
        MockHttpServer.stopServer();
    }

    @BeforeEach
    void pactSetup(PactVerificationContext context) throws IOException {
        environmentVariables.set("LAMBDA_TASK_ROOT", "handler");

        setupEventProbeBehaviour();

        MockHttpServer.startServer(
                new ArrayList<>(List.of(new Injector(handler, "/credential/issue", "/"))), PORT);

        context.setTarget(new HttpTestTarget("localhost", PORT));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTest(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("dummyInvalidAccessToken is an invalid access token")
    public void inValidDummyAccessToken() throws ParseException {
        AccessToken accessToken =
                AccessToken.parse("Bearer dummyInvalidAccessToken", AccessTokenType.BEARER);
        when(sessionServiceMock.getSessionByAccessToken(accessToken))
                .thenThrow(new AccessTokenExpiredException("access code expired"));
    }

    private void setupEventProbeBehaviour() {
        when(eventProbeMock.counterMetric(KBV_CREDENTIAL_ISSUER, 0d)).thenReturn(eventProbeMock);
        when(eventProbeMock.log(eq(Level.ERROR), Mockito.any(Exception.class)))
                .thenReturn(eventProbeMock);
    }
}
