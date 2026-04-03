package com.peatroxd.mtprototest.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PublicCatalogCacheService {

    private final CacheManager cacheManager;

    public PublicCatalogCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictPublicCatalogViews() {
        evictAll(PublicCatalogCacheNames.PROXY_BEST);
        evictAll(PublicCatalogCacheNames.PROXY_STATS);
    }

    public void evictProxyById(Long proxyId) {
        if (proxyId == null) {
            return;
        }

        Cache cache = cacheManager.getCache(PublicCatalogCacheNames.PROXY_BY_ID);
        if (cache != null) {
            cache.evict(proxyId);
        }
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
