package uk.gov.di.ipv.cri.kbv.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGateway;
import uk.gov.di.ipv.cri.kbv.api.gateway.KeyStoreLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceFactoryTest {
    @Mock private ClientProviderFactory clientProviderFactory;
    @Mock private SSMProvider ssmProvider;
    @Mock private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    @Mock private SecretsProvider secretsProvider;
    @Mock private SqsClient sqsClient;
    @InjectMocks private ServiceFactory serviceFactory;

    @Test
    void testGetSsmProvider() {
        when(clientProviderFactory.getSSMProvider()).thenReturn(ssmProvider);

        SSMProvider result = serviceFactory.getSsmProvider();

        assertNotNull(result);
        verify(clientProviderFactory, times(3)).getSSMProvider();
    }

    @Test
    void testGetDynamoDbEnhancedClient() {
        when(clientProviderFactory.getDynamoDbEnhancedClient()).thenReturn(dynamoDbEnhancedClient);

        DynamoDbEnhancedClient result = serviceFactory.getDynamoDbEnhancedClient();

        assertNotNull(result);
        verify(clientProviderFactory).getDynamoDbEnhancedClient();
    }

    @Test
    void testGetSecretsProvider() {
        when(clientProviderFactory.getSecretsProvider()).thenReturn(secretsProvider);

        SecretsProvider result = serviceFactory.getSecretsProvider();

        assertNotNull(result);
        verify(clientProviderFactory, times(3)).getSecretsProvider();
    }

    @Test
    void testGetSqsClient() {
        when(clientProviderFactory.getSqsClient()).thenReturn(sqsClient);

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
        KBVGateway kbvGateway = serviceFactory.getKbvGateway(mock(KeyStoreLoader.class));

        assertNotNull(kbvGateway);
    }

    @Test
    void testGetKbvGatewayReturnsExceptionWhenCreationFails() {
        assertThrows(KBVGatewayCreationException.class, serviceFactory::getKbvGateway);
    }
}
