package uk.gov.di.ipv.cri.kbv.api.security;

import com.github.benmanes.caffeine.cache.CacheLoader;

public class Base64TokenCacheLoader implements CacheLoader<String, Base64TokenEncoder> {
    private final SoapToken soapToken;

    public Base64TokenCacheLoader(SoapToken soapToken) {
        this.soapToken = soapToken;
    }

    @Override
    public Base64TokenEncoder load(String key) throws Exception {
        return new Base64TokenEncoder(key, soapToken);
    }
}
