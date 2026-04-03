package com.peatroxd.mtprototest.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class PublicCatalogCacheServiceTest {

    @Test
    void shouldEvictCachedPublicViewsAndProxyDetails() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                PublicCatalogCacheNames.PROXY_BEST,
                PublicCatalogCacheNames.PROXY_STATS,
                PublicCatalogCacheNames.PROXY_BY_ID
        );
        PublicCatalogCacheService service = new PublicCatalogCacheService(cacheManager);

        cacheManager.getCache(PublicCatalogCacheNames.PROXY_BEST).put("key", "best");
        cacheManager.getCache(PublicCatalogCacheNames.PROXY_STATS).put("key", "stats");
        cacheManager.getCache(PublicCatalogCacheNames.PROXY_BY_ID).put(42L, "proxy");

        service.evictProxyById(42L);
        service.evictPublicCatalogViews();

        assertThat(cacheManager.getCache(PublicCatalogCacheNames.PROXY_BEST).get("key")).isNull();
        assertThat(cacheManager.getCache(PublicCatalogCacheNames.PROXY_STATS).get("key")).isNull();
        assertThat(cacheManager.getCache(PublicCatalogCacheNames.PROXY_BY_ID).get(42L)).isNull();
    }
}
