package com.peatroxd.mtprototest.common.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class PublicCatalogCacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                PublicCatalogCacheNames.PROXY_BEST,
                PublicCatalogCacheNames.PROXY_STATS,
                PublicCatalogCacheNames.PROXY_BY_ID
        );
    }
}
