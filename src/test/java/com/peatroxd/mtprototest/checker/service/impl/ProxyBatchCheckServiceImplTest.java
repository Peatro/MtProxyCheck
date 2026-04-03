package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.config.CheckerExecutorProperties;
import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.ProxyCheckExecutionService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyBatchCheckServiceImplTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyCheckExecutionService proxyCheckExecutionService;
    @Mock
    private ProxyCheckUpdateService proxyCheckUpdateService;
    @Mock
    private ProxyMetricsService proxyMetricsService;
    @Mock
    private PublicCatalogCacheService publicCatalogCacheService;

    @Test
    void shouldProcessLargeCatalogInSubmissionWavesWithoutRejectingExecutorTasks() {
        CheckerProperties checkerProperties = new CheckerProperties();
        checkerProperties.setDeepProbeLimit(5);

        CheckerExecutorProperties executorProperties = new CheckerExecutorProperties();
        executorProperties.setCorePoolSize(1);
        executorProperties.setMaxPoolSize(1);
        executorProperties.setQueueCapacity(1);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProperties.getCorePoolSize());
        executor.setMaxPoolSize(executorProperties.getMaxPoolSize());
        executor.setQueueCapacity(executorProperties.getQueueCapacity());
        executor.initialize();

        ProxyBatchCheckServiceImpl service = new ProxyBatchCheckServiceImpl(
                proxyRepository,
                proxyCheckExecutionService,
                proxyCheckUpdateService,
                executor,
                executorProperties,
                checkerProperties,
                proxyMetricsService,
                publicCatalogCacheService
        );

        List<ProxyEntity> proxies = List.of(
                proxy(1L), proxy(2L), proxy(3L), proxy(4L), proxy(5L)
        );

        when(proxyRepository.findAllByOrderByIdAsc()).thenReturn(proxies);
        when(proxyCheckExecutionService.execute(any(ProxyEntity.class), eq(true))).thenAnswer(invocation -> execution());

        try {
            ProxyBatchCheckSummary summary = service.checkAllProxies();

            assertThat(summary.totalChecked()).isEqualTo(5);
            assertThat(summary.verifiedCount()).isEqualTo(5);
            assertThat(summary.quickOkCount()).isZero();
            assertThat(summary.deadCount()).isZero();
            verify(proxyCheckUpdateService, times(5)).applyExecution(any(Long.class), any(ProxyCheckExecution.class));
            verify(publicCatalogCacheService).evictPublicCatalogViews();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldUseDedicatedStartupBudgetForStartupChecks() {
        CheckerProperties checkerProperties = new CheckerProperties();
        checkerProperties.setDeepProbeLimit(5);

        CheckerExecutorProperties executorProperties = new CheckerExecutorProperties();
        executorProperties.setMaxPoolSize(4);
        executorProperties.setEffectiveConcurrency(1);
        executorProperties.setSubmissionWindowSize(1);

        ProxyBatchCheckServiceImpl service = service(executorProperties, checkerProperties, Runnable::run);

        List<ProxyEntity> proxies = List.of(proxy(11L), proxy(12L), proxy(13L));

        when(proxyRepository.findStartupBatch(any())).thenReturn(proxies.subList(0, 2));
        when(proxyCheckExecutionService.execute(any(ProxyEntity.class), eq(true))).thenAnswer(invocation -> execution());
        when(proxyCheckExecutionService.execute(any(ProxyEntity.class), eq(false))).thenAnswer(invocation -> execution());

        ProxyBatchCheckSummary summary = service.checkStartupProxies(2, 1);

        assertThat(summary.totalChecked()).isEqualTo(2);
        verify(proxyRepository).findStartupBatch(any());
        verify(proxyCheckExecutionService, times(1)).execute(any(ProxyEntity.class), eq(true));
        verify(proxyCheckExecutionService, times(1)).execute(any(ProxyEntity.class), eq(false));
    }

    private ProxyBatchCheckServiceImpl service(
            CheckerExecutorProperties executorProperties,
            CheckerProperties checkerProperties,
            Executor executor
    ) {
        return new ProxyBatchCheckServiceImpl(
                proxyRepository,
                proxyCheckExecutionService,
                proxyCheckUpdateService,
                executor,
                executorProperties,
                checkerProperties,
                proxyMetricsService,
                publicCatalogCacheService
        );
    }

    private ProxyCheckExecution execution() {
        return new ProxyCheckExecution(
                new ProxyCheckResult(true, 80L, ProxyVerificationStatus.VERIFIED, null, null),
                List.of()
        );
    }

    private ProxyEntity proxy(Long id) {
        return ProxyEntity.builder()
                .id(id)
                .host("host" + id)
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.QUICK_OK)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(50)
                .consecutiveFailures(0)
                .consecutiveSuccesses(0)
                .build();
    }
}
