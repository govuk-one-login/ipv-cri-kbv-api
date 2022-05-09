package uk.gov.di.ipv.cri.experian.kbv.api.security;

import com.experian.uk.wasp.TokenService;
import com.github.benmanes.caffeine.cache.CacheLoader;

public class Base64TokenCacheLoader implements CacheLoader<String, Base64TokenEncoder> {
    @Override
    public Base64TokenEncoder load(String key) throws Exception {
        return new Base64TokenEncoder(key, new SoapToken("GDS DI", true, new TokenService()));
    }
}
