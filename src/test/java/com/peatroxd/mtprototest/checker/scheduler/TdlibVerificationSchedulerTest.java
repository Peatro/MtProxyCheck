package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.client.ProxyPingAgentClient;
import com.peatroxd.mtprototest.checker.client.ProxyPingAgentClient.PingResult;
import com.peatroxd.mtprototest.checker.config.TdlibProperties;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TdlibVerificationSchedulerTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyPingAgentClient agentClient;
    @Mock
    private ProxyCheckUpdateService proxyCheckUpdateService;
    @Mock
    private ProxyMetricsService proxyMetricsService;

    private TdlibVerificationScheduler scheduler(TdlibProperties properties) {
        return new TdlibVerificationScheduler(
                proxyRepository,
                agentClient,
                proxyCheckUpdateService,
                proxyMetricsService,
                properties,
                (Runnable command) -> command.run()   // синхронный executor
        );
    }

    @Test
    void skipsCycleWhenAgentUnreachable() {
        when(agentClient.healthy()).thenReturn(false);

        scheduler(new TdlibProperties()).verifyEndToEnd();

        verifyNoInteractions(proxyRepository, proxyCheckUpdateService);
        verify(agentClient, never()).probe(any(), anyInt(), any());
    }

    @Test
    void probesCandidatesAndAppliesResults() {
        TdlibProperties properties = new TdlibProperties();
        properties.setBatchLimit(4);

        when(agentClient.healthy()).thenReturn(true);
        when(proxyRepository.findE2ePromotionCandidates(any()))
                .thenReturn(List.of(proxy(1L, ProxyVerificationStatus.PROTOCOL_OK)));
        when(proxyRepository.findE2eRecheckCandidates(any(), any()))
                .thenReturn(List.of(proxy(2L, ProxyVerificationStatus.VERIFIED)));
        when(agentClient.probe(any(), anyInt(), any()))
                .thenReturn(new PingResult(true, 0.12, null));

        scheduler(properties).verifyEndToEnd();

        verify(proxyCheckUpdateService).applyE2eResult(eq(1L), eq(true), any(), any());
        verify(proxyCheckUpdateService).applyE2eResult(eq(2L), eq(true), any(), any());
        verify(proxyMetricsService, times(2)).incrementE2eCheck(true);
    }

    private ProxyEntity proxy(Long id, ProxyVerificationStatus verificationStatus) {
        return ProxyEntity.builder()
                .id(id)
                .host("host" + id)
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
                .e2eFailureStreak(0)
                .build();
    }
}
