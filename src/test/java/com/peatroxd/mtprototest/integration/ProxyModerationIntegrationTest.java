package com.peatroxd.mtprototest.integration;

import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProxyModerationIntegrationTest {

    @Autowired
    private com.peatroxd.mtprototest.proxy.repository.ProxyRepository proxyRepository;
    @Autowired
    private ProxyService proxyService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PublicCatalogCacheService publicCatalogCacheService;

    @Test
    void shouldExcludeBlacklistedProxyFromPublicCatalogAndPrioritizeWhitelisted() {
        proxyRepository.save(ProxyEntity.builder()
                .host("40.40.40.40")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("moderation")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.VERIFIED)
                .moderationStatus(ProxyModerationStatus.BLACKLISTED)
                .score(99)
                .consecutiveFailures(0)
                .consecutiveSuccesses(2)
                .build());

        proxyRepository.save(ProxyEntity.builder()
                .host("41.41.41.41")
                .port(443)
                .secret("00112233445566778899aabbccddeefe")
                .type(ProxyType.MTPROTO)
                .source("moderation")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.VERIFIED)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(95)
                .consecutiveFailures(0)
                .consecutiveSuccesses(2)
                .build());

        proxyRepository.save(ProxyEntity.builder()
                .host("42.42.42.42")
                .port(443)
                .secret("00112233445566778899aabbccddeefe")
                .type(ProxyType.MTPROTO)
                .source("moderation")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.QUICK_OK)
                .moderationStatus(ProxyModerationStatus.WHITELISTED)
                .score(80)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build());

        entityManager.flush();
        entityManager.clear();
        publicCatalogCacheService.evictPublicCatalogViews();

        var best = proxyService.getBest();
        var stats = proxyService.getStats();

        assertThat(best).extracting(proxy -> proxy.host()).doesNotContain("40.40.40.40");
        assertThat(best.getFirst().host()).isEqualTo("42.42.42.42");
        assertThat(stats.totalProxies()).isEqualTo(2);
    }
}
