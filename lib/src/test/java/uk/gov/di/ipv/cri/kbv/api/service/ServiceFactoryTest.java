package uk.gov.di.ipv.cri.kbv.api.service;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.KeyStoreLoader;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceFactoryTest {
    @Mock private ClientProviderFactory clientProviderFactory;
    @Mock private SSMProvider ssmProvider;
    @Mock private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    @Mock private SecretsProvider secretsProvider;
    @Mock private SqsClient sqsClient;

    @InjectMocks private ServiceFactory serviceFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clientProviderFactory.getSSMProvider()).thenReturn(ssmProvider);
        when(clientProviderFactory.getDynamoDbEnhancedClient()).thenReturn(dynamoDbEnhancedClient);
        when(clientProviderFactory.getSecretsProvider()).thenReturn(secretsProvider);
        when(clientProviderFactory.getSqsClient()).thenReturn(sqsClient);
    }

    @Test
    void testGetSsmProvider() {
        SSMProvider result = serviceFactory.getSsmProvider();
        assertNotNull(result);
        verify(clientProviderFactory).getSSMProvider();
    }

    @Test
    void testGetDynamoDbEnhancedClient() {
        DynamoDbEnhancedClient result = serviceFactory.getDynamoDbEnhancedClient();
        assertNotNull(result);
        verify(clientProviderFactory).getDynamoDbEnhancedClient();
    }

    @Test
    void testGetSecretsProvider() {
        SecretsProvider result = serviceFactory.getSecretsProvider();
        assertNotNull(result);
        verify(clientProviderFactory).getSecretsProvider();
    }

    @Test
    void testGetSqsClient() {
        SqsClient result = serviceFactory.getSqsClient();
        assertNotNull(result);
        verify(clientProviderFactory).getSqsClient();
    }

    @Test
    void testGetAuditServiceThrowsWhenEnvironmentNotSetupCorrectly() {
        boolean hasThrown = false;
        try {
            serviceFactory.getAuditService();
        } catch (Exception e) {
            hasThrown = true;
        }
        assertTrue(hasThrown);
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
    void testGetKbvGateway() {
        KBVClientFactory kbvClientFactoryMock = mock(KBVClientFactory.class);
        KeyStoreLoader keyStoreLoaderMock = mock(KeyStoreLoader.class);
        IdentityIQWebServiceSoap identityIQWebServiceSoapMock =
                mock(IdentityIQWebServiceSoap.class);

        when(kbvClientFactoryMock.createClient()).thenReturn(identityIQWebServiceSoapMock);

        KBVGateway kbvGateway =
                serviceFactory.getKbvGateway(keyStoreLoaderMock, kbvClientFactoryMock);

        assertNotNull(kbvGateway);
    }

    @Test
    void testGetKbvGatewayReturnsExceptionWhenCreationFails() {

        KBVGatewayCreationException exception =
                assertThrows(KBVGatewayCreationException.class, serviceFactory::getKbvGateway);

        assertEquals(
                "Failed to create KBVGateway: Persist keystore to file failed: Cannot invoke \"String.getBytes(java.nio.charset.Charset)\" because \"src\" is null",
                exception.getMessage());
    }
}
