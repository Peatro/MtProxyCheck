package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.config.FeedbackProperties;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyFeedbackRepository;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyCheckUpdateServiceImplE2eTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    @Mock
    private ProxyFeedbackRepository proxyFeedbackRepository;
    @Mock
    private FeedbackProperties feedbackProperties;
    @Mock
    private ProxyScoringService proxyScoringService;
    @Mock
    private PublicCatalogCacheService publicCatalogCacheService;

    private ProxyCheckUpdateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProxyCheckUpdateServiceImpl(
                proxyRepository,
                proxyCheckHistoryRepository,
                proxyFeedbackRepository,
                feedbackProperties,
                proxyScoringService,
                publicCatalogCacheService
        );
        lenient().when(feedbackProperties.getRecentLimit()).thenReturn(20);
        lenient().when(proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(any()))
                .thenReturn(List.<ProxyCheckHistoryEntity>of());
        lenient().when(proxyFeedbackRepository.findTop20ByProxyIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());
        lenient().when(proxyScoringService.calculateScore(any())).thenReturn(70);
    }

    @Test
    void e2eOkPromotesProtocolOkToVerified() {
        ProxyEntity proxy = proxy(ProxyVerificationStatus.PROTOCOL_OK, 2);
        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));

        service.applyE2eResult(1L, true, 120L, null);

        assertThat(proxy.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.VERIFIED);
        assertThat(proxy.getE2eFailureStreak()).isZero();
        assertThat(proxy.getStatus()).isEqualTo(ProxyStatus.ALIVE);
        assertThat(proxy.getLastSuccessAt()).isNotNull();
    }

    @Test
    void e2eOkKeepsVerifiedAndResetsStreak() {
        ProxyEntity proxy = proxy(ProxyVerificationStatus.VERIFIED, 1);
        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));

        service.applyE2eResult(1L, true, 90L, null);

        assertThat(proxy.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.VERIFIED);
        assertThat(proxy.getE2eFailureStreak()).isZero();
    }

    @Test
    void e2eFailBelow3HoldsStatus() {
        ProxyEntity proxy = proxy(ProxyVerificationStatus.VERIFIED, 0);
        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));

        service.applyE2eResult(1L, false, null, "timeout");

        assertThat(proxy.getE2eFailureStreak()).isEqualTo(1);
        assertThat(proxy.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.VERIFIED);
        assertThat(proxy.getStatus()).isEqualTo(ProxyStatus.ALIVE);
    }

    @Test
    void e2eThirdFailDemotesVerifiedToProtocolOk() {
        ProxyEntity proxy = proxy(ProxyVerificationStatus.VERIFIED, 2);
        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));

        service.applyE2eResult(1L, false, null, "timeout");

        assertThat(proxy.getE2eFailureStreak()).isEqualTo(3);
        assertThat(proxy.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.PROTOCOL_OK);
        assertThat(proxy.getStatus()).isEqualTo(ProxyStatus.ALIVE);
    }

    @Test
    void e2eFifthFailMarksDead() {
        ProxyEntity proxy = proxy(ProxyVerificationStatus.PROTOCOL_OK, 4);
        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));

        service.applyE2eResult(1L, false, null, "timeout");

        assertThat(proxy.getStatus()).isEqualTo(ProxyStatus.DEAD);
        assertThat(proxy.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.UNVERIFIED);
        assertThat(proxy.getE2eFailureStreak()).isZero();
    }

    private ProxyEntity proxy(ProxyVerificationStatus verificationStatus, int e2eFailureStreak) {
        return ProxyEntity.builder()
                .id(1L)
                .host("host1")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(verificationStatus)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(50)
                .consecutiveFailures(0)
                .consecutiveSuccesses(0)
                .e2eFailureStreak(e2eFailureStreak)
                .build();
    }
}
