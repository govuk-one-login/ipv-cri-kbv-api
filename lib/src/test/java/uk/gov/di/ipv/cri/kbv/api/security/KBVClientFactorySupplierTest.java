package uk.gov.di.ipv.cri.kbv.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class KBVClientFactorySupplierTest {
    @Mock private ConfigurationService configurationService;
    @Mock private SoapTokenRetriever soapTokenRetriever;
    @InjectMocks
    KBVClientFactorySupplier factoryCreator;

    @Test
    void returnsNonNullSupplier() {
        Supplier<KBVClientFactory> factorySupplier = factoryCreator.getKbvClientFactory(soapTokenRetriever);

        assertNotNull(factorySupplier);
    }

    @Test
    void returnsNonNullInstance() {
        Supplier<KBVClientFactory> factorySupplier = factoryCreator.getKbvClientFactory(soapTokenRetriever);

        assertNotNull(factorySupplier.get());
    }
}
