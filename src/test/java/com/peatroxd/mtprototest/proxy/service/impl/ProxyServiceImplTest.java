package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.mapper.ProxyResponseMapper;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyServiceImplTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;

    @Test
    void shouldRankVerifiedProxyAheadOfQuickOk() {
        ProxyServiceImpl service = new ProxyServiceImpl(proxyRepository, proxyCheckHistoryRepository, new ProxyResponseMapper());

        ProxyEntity quickOk = proxy(1L, ProxyVerificationStatus.QUICK_OK, 95, 100L);
        ProxyEntity verified = proxy(2L, ProxyVerificationStatus.VERIFIED, 90, 140L);
        when(proxyRepository.findTop200ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus.ALIVE))
                .thenReturn(List.of(quickOk, verified));

        var best = service.getBest();

        assertThat(best).hasSize(2);
        assertThat(best.getFirst().id()).isEqualTo(2L);
    }

    private ProxyEntity proxy(Long id, ProxyVerificationStatus verificationStatus, int score, long latencyMs) {
        return ProxyEntity.builder()
                .id(id)
                .host("host" + id)
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(verificationStatus)
                .score(score)
                .lastLatencyMs(latencyMs)
                .lastSuccessAt(LocalDateTime.now())
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build();
    }
}
