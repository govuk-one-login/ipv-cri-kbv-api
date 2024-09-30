package uk.gov.di.ipv.cri.kbv.api.security;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class HeaderHandlerProvider {

    private final Base64TokenCacheLoader cacheLoader;

    public HeaderHandlerProvider(Base64TokenCacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    public HeaderHandler getHeaderHandler() {
        return new HeaderHandler(
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.HOURS).build(this.cacheLoader));
    }
}
