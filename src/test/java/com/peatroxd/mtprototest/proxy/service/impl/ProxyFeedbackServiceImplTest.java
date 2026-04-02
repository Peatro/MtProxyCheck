package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.proxy.config.FeedbackProperties;
import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyFeedbackRepository;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyFeedbackServiceImplTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyFeedbackRepository proxyFeedbackRepository;
    @Mock
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    @Mock
    private ProxyScoringService proxyScoringService;

    @Test
    void shouldRejectDuplicateFeedbackInSameWindow() {
        FeedbackProperties properties = new FeedbackProperties();
        ProxyFeedbackServiceImpl service = new ProxyFeedbackServiceImpl(
                proxyRepository,
                proxyFeedbackRepository,
                proxyCheckHistoryRepository,
                proxyScoringService,
                properties
        );

        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy()));
        when(proxyFeedbackRepository.existsByProxyIdAndPlatformAndClientKeyAndWindowStartedAt(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.submitFeedback(
                1L,
                new ProxyFeedbackRequest(ProxyFeedbackResult.WORKED, ProxyFeedbackPlatform.DESKTOP),
                "fingerprint"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void shouldPersistFeedbackAndRecalculateScore() {
        FeedbackProperties properties = new FeedbackProperties();
        ProxyFeedbackServiceImpl service = new ProxyFeedbackServiceImpl(
                proxyRepository,
                proxyFeedbackRepository,
                proxyCheckHistoryRepository,
                proxyScoringService,
                properties
        );
        ProxyEntity proxy = proxy();

        when(proxyRepository.findById(1L)).thenReturn(Optional.of(proxy));
        when(proxyFeedbackRepository.existsByProxyIdAndPlatformAndClientKeyAndWindowStartedAt(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(false);
        when(proxyFeedbackRepository.findTop20ByProxyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(1L)).thenReturn(List.of());
        when(proxyScoringService.calculateScore(any())).thenReturn(77);

        service.submitFeedback(
                1L,
                new ProxyFeedbackRequest(ProxyFeedbackResult.WORKED, ProxyFeedbackPlatform.DESKTOP),
                "fingerprint"
        );

        verify(proxyFeedbackRepository).save(any());
        verify(proxyRepository).save(ArgumentMatchers.argThat(saved -> saved.getScore() == 77));
    }

    private ProxyEntity proxy() {
        return ProxyEntity.builder()
                .id(1L)
                .host("host")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.QUICK_OK)
                .score(0)
                .consecutiveFailures(0)
                .consecutiveSuccesses(0)
                .build();
    }
}
