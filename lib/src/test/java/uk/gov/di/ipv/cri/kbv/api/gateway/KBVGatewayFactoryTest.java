package uk.gov.di.ipv.cri.kbv.api.gateway;

import com.experian.uk.schema.experian.identityiq.services.webservice.IdentityIQWebServiceSoap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;
import uk.gov.di.ipv.cri.kbv.api.security.KBVClientFactory;
import uk.gov.di.ipv.cri.kbv.api.security.SoapTokenRetriever;

import java.io.IOException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KBVGatewayFactoryTest {
    private Supplier<SoapTokenRetriever> mockSoapTokenRetrieverSupplier;
    private Supplier<KBVClientFactory> mockKbvClientFactorySupplier;
    @Mock private KeyStoreLoader keyStoreLoaderMock;
    @Mock private KBVClientFactory kbvClientFactoryMock;
    @Mock private SoapTokenRetriever soapTokenRetrieverMock;
    @InjectMocks private KBVGatewayFactory kbvGatewayFactory;

    @BeforeEach
    void setUp() {
        mockSoapTokenRetrieverSupplier = () -> soapTokenRetrieverMock;
        mockKbvClientFactorySupplier = () -> kbvClientFactoryMock;
    }

    @Test
    void shouldCreateKbvGatewaySuccessfully() throws IOException {
        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);

        IdentityIQWebServiceSoap identityIQWebServiceSoapMock =
                mock(IdentityIQWebServiceSoap.class);

        doNothing().when(keyStoreLoaderMock).load();
        when(kbvClientFactoryMock.createClient()).thenReturn(identityIQWebServiceSoapMock);

        kbvGatewayFactory =
                new KBVGatewayFactory(
                        keyStoreLoaderMock,
                        mockKbvClientFactorySupplier,
                        configurationServiceMock,
                        mockSoapTokenRetrieverSupplier);

        KBVGateway kbvGateway = kbvGatewayFactory.create();

        assertNotNull(kbvGateway);
        verify(keyStoreLoaderMock).load();
    }

    @Test
    void shouldThrowKbvGatewayCreationExceptionWhenKeyStoreLoaderFails() throws IOException {
        doThrow(new RuntimeException("KeyStore loading failed")).when(keyStoreLoaderMock).load();

        KBVGatewayCreationException exception =
                assertThrows(KBVGatewayCreationException.class, kbvGatewayFactory::create);

        assertEquals(
                "Failed to create KBVGateway: KeyStore loading failed", exception.getMessage());
        verify(keyStoreLoaderMock).load();
    }
}
