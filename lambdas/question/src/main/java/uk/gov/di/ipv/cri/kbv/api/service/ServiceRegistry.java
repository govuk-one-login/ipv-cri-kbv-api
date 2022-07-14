package uk.gov.di.ipv.cri.kbv.api.service;

import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityService;
import uk.gov.di.ipv.cri.kbv.api.gateway.KBVGatewayFactory;

public class ServiceRegistry {
    private final ConfigurationService configurationService;

    public ServiceRegistry(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public KBVService getKBVService() {
        return new KBVService(new KBVGatewayFactory().create(this.configurationService));
    }

    public KBVStorageService getKBVStorageService() {
        return new KBVStorageService(this.configurationService);
    }

    public PersonIdentityService getPersonIdentityService() {
        return new PersonIdentityService();
    }
}
