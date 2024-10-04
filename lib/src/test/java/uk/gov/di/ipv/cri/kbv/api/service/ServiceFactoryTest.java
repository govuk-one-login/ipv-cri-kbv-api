package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebService;
import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.kbv.api.exception.InvalidSoapTokenException;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.KeyStoreLoader;
import uk.gov.di.ipv.cri.kbv.api.security.HeaderHandlerResolver;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

import javax.xml.ws.BindingProvider;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ServiceFactoryTest {
    @Mock private ConfigurationService configurationServiceMock;
    @Mock private ClientProviderFactory clientProviderFactoryMock;
    @Mock private KeyStoreLoader keyStoreLoaderMock;
    @Mock private IdentityIQWebServiceSoap identityIQWebServiceSoapMock;
    @Mock private KBVClientFactory kbvClientFactoryMock;
    @Mock private SSMProvider ssmProviderMock;
    @Mock private DynamoDbEnhancedClient dynamoDbEnhancedClientMock;
    @Mock private SecretsProvider secretsProviderMock;
    @Mock private SqsClient sqsClientMock;

    @InjectMocks private ServiceFactory serviceFactory;

    @Test
    void testGetSsmProvider() {
        when(clientProviderFactoryMock.getSSMProvider()).thenReturn(ssmProviderMock);
        SSMProvider result = serviceFactory.getSsmProvider();

        assertNotNull(result);
        verify(clientProviderFactoryMock).getSSMProvider();
    }

    @Test
    void testGetDynamoDbEnhancedClient() {
        when(clientProviderFactoryMock.getDynamoDbEnhancedClient())
                .thenReturn(dynamoDbEnhancedClientMock);
        DynamoDbEnhancedClient result = serviceFactory.getDynamoDbEnhancedClient();

        assertNotNull(result);
        verify(clientProviderFactoryMock).getDynamoDbEnhancedClient();
    }

    @Test
    void testGetSecretsProvider() {
        when(clientProviderFactoryMock.getSecretsProvider()).thenReturn(secretsProviderMock);
        SecretsProvider result = serviceFactory.getSecretsProvider();

        assertNotNull(result);
        verify(clientProviderFactoryMock).getSecretsProvider();
    }

    @Test
    void testGetSqsClient() {
        when(clientProviderFactoryMock.getSqsClient()).thenReturn(sqsClientMock);
        SqsClient result = serviceFactory.getSqsClient();

        assertNotNull(result);
        verify(clientProviderFactoryMock).getSqsClient();
    }

    @Test
    void testGetAuditService() {
        when(configurationServiceMock.getSqsAuditEventPrefix()).thenReturn("auditPrefix");
        when(configurationServiceMock.getSqsAuditEventQueueUrl())
                .thenReturn("http://audit-event-url");
        when(configurationServiceMock.getVerifiableCredentialIssuer()).thenReturn("an-issuer");
        when(clientProviderFactoryMock.getSqsClient()).thenReturn(sqsClientMock);
        serviceFactory.getConfigurationService();

        AuditService auditService = serviceFactory.getAuditService();

        verify(configurationServiceMock).getSqsAuditEventPrefix();
        verify(configurationServiceMock).getSqsAuditEventQueueUrl();
        verify(configurationServiceMock).getVerifiableCredentialIssuer();
        verify(clientProviderFactoryMock).getSqsClient();
        assertNotNull(auditService);
    }

    @Test
    void testGetAuditServiceThrowsWhenAuditAttributesNotSetupCorrectly() {
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> serviceFactory.getAuditService());

        assertEquals(
                "Audit event prefix not retrieved from configuration service",
                exception.getMessage());
    }

    @Test
    void testGetConfigurationService() {
        ConfigurationService configurationService1 = serviceFactory.getConfigurationService();
        assertNotNull(configurationService1);
        ConfigurationService configurationService2 = serviceFactory.getConfigurationService();
        assertNotNull(configurationService2);
        assertEquals(configurationService1, configurationService2);
    }

    @Test
    void testGetSessionService() {
        when(clientProviderFactoryMock.getDynamoDbEnhancedClient())
                .thenReturn(dynamoDbEnhancedClientMock);

        SessionService result = serviceFactory.getSessionService();

        verify(clientProviderFactoryMock).getDynamoDbEnhancedClient();
        assertNotNull(result);
    }

    @Test
    void testGetKbvGatewayReturnsSameInstanceOnMultipleInitialization() throws IOException {
        when(kbvClientFactoryMock.createClient()).thenReturn(identityIQWebServiceSoapMock);
        doNothing().when(keyStoreLoaderMock).load();

        serviceFactory.setKbvGateway(keyStoreLoaderMock);
        KBVGateway firstCall = serviceFactory.getKbvGateway();
        KBVGateway secondCall = serviceFactory.getKbvGateway();

        verify(kbvClientFactoryMock).createClient();
        verify(keyStoreLoaderMock).load();
        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void testGetKbvGatewayWhenGatewayIsNullShouldInitializeKbvGateway() throws IOException {
        when(kbvClientFactoryMock.createClient()).thenReturn(identityIQWebServiceSoapMock);
        doNothing().when(keyStoreLoaderMock).load();

        serviceFactory.setKbvGateway(keyStoreLoaderMock);
        KBVGateway result = serviceFactory.getKbvGateway();

        assertNotNull(result);
        verify(kbvClientFactoryMock).createClient();
        verify(keyStoreLoaderMock).load();
    }

    @Test
    void testGetKbvGatewayReturnsAnInstanceUsingKbvClientFactoryWithMockedDependencies() {
        identityIQWebServiceSoapMock =
                mock(
                        IdentityIQWebServiceSoap.class,
                        withSettings().extraInterfaces(BindingProvider.class));
        IdentityIQWebService identityIQWebServiceMock = mock(IdentityIQWebService.class);

        when(identityIQWebServiceMock.getIdentityIQWebServiceSoap())
                .thenReturn(identityIQWebServiceSoapMock);

        serviceFactory.setKBVClientFactory(
                identityIQWebServiceMock, mock(HeaderHandlerResolver.class));
        serviceFactory.setKbvGateway(keyStoreLoaderMock);

        assertNotNull(serviceFactory.getKbvGateway());
        verify(identityIQWebServiceMock).getIdentityIQWebServiceSoap();
    }

    @Test
    void testGetKbvGatewayThrowsKBVGatewayCreationExceptionOnSoapFault() {
        when(kbvClientFactoryMock.createClient())
                .thenThrow(new InvalidSoapTokenException("SOAP Fault occurred"));

        KBVGatewayCreationException exception =
                assertThrows(
                        KBVGatewayCreationException.class,
                        () -> serviceFactory.setKbvGateway(keyStoreLoaderMock));

        assertEquals("Failed to create KBVGateway: SOAP Fault occurred", exception.getMessage());
        verify(kbvClientFactoryMock).createClient();
    }
}
