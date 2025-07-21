package uk.gov.di.ipv.cri.kbv.api.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.kbv.api.exception.KBVGatewayCreationException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KBVGatewayFactoryTest {
    @Mock private KeyStoreLoader keyStoreLoaderMock;
    @InjectMocks private KBVGatewayFactory kbvGatewayFactory;

    @Test
    void shouldCreateKbvGatewaySuccessfully() throws IOException {
        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);

        doNothing().when(keyStoreLoaderMock).load();

        kbvGatewayFactory = new KBVGatewayFactory(keyStoreLoaderMock, configurationServiceMock);

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
