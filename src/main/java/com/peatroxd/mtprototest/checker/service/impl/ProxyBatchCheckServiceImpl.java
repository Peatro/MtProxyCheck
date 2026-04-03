package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.config.CheckerExecutorProperties;
import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckHistoryRecord;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckExecutionService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyBatchCheckServiceImpl implements ProxyBatchCheckService {

    private final ProxyRepository proxyRepository;
    private final ProxyCheckExecutionService proxyCheckExecutionService;
    private final ProxyCheckUpdateService proxyCheckUpdateService;
    private final Executor proxyCheckerExecutor;
    private final CheckerExecutorProperties checkerExecutorProperties;
    private final CheckerProperties checkerProperties;
    private final ProxyMetricsService proxyMetricsService;
    private final PublicCatalogCacheService publicCatalogCacheService;

    @Override
    public ProxyBatchCheckSummary checkAllProxies() {
        return checkAndRefreshCatalog(
                proxyRepository.findAllByOrderByIdAsc(),
                "all proxies",
                checkerProperties.getDeepProbeLimit()
        );
    }

    @Override
    public ProxyBatchCheckSummary checkStartupProxies(int batchSize, int deepProbeLimit) {
        return checkAndRefreshCatalog(
                proxyRepository.findStartupBatch(PageRequest.of(0, batchSize)),
                "startup candidate proxies",
                deepProbeLimit
        );
    }

    @Override
    public ProxyBatchCheckSummary checkNewProxies() {
        return checkAndRefreshCatalog(
                proxyRepository.findLifecycleBatchByStatus(
                        ProxyStatus.NEW,
                        PageRequest.of(0, checkerProperties.getBatchSize())
                ),
                "NEW proxies",
                checkerProperties.getDeepProbeLimit()
        );
    }

    @Override
    public ProxyBatchCheckSummary checkAliveQuickOkProxies() {
        return checkAndRefreshCatalog(
                proxyRepository.findLifecycleBatchByStatusAndVerificationStatus(
                        ProxyStatus.ALIVE,
                        ProxyVerificationStatus.QUICK_OK,
                        LocalDateTime.now().minusNanos(checkerProperties.getAliveQuickOkRecheckAfterMs() * 1_000_000),
                        PageRequest.of(0, checkerProperties.getBatchSize())
                ),
                "ALIVE QUICK_OK proxies",
                checkerProperties.getDeepProbeLimit()
        );
    }

    @Override
    public ProxyBatchCheckSummary checkAliveVerifiedProxies() {
        return checkAndRefreshCatalog(
                proxyRepository.findLifecycleBatchByStatusAndVerificationStatus(
                        ProxyStatus.ALIVE,
                        ProxyVerificationStatus.VERIFIED,
                        LocalDateTime.now().minusNanos(checkerProperties.getAliveVerifiedRecheckAfterMs() * 1_000_000),
                        PageRequest.of(0, checkerProperties.getBatchSize())
                ),
                "ALIVE VERIFIED proxies",
                checkerProperties.getDeepProbeLimit()
        );
    }

    @Override
    public ProxyBatchCheckSummary checkDeadProxies() {
        return checkAndRefreshCatalog(
                proxyRepository.findLifecycleBatchByStatusBefore(
                        ProxyStatus.DEAD,
                        LocalDateTime.now().minusNanos(checkerProperties.getDeadRetryAfterMs() * 1_000_000),
                        PageRequest.of(0, checkerProperties.getBatchSize())
                ),
                "DEAD proxies",
                checkerProperties.getDeepProbeLimit()
        );
    }

    private ProxyBatchCheckSummary checkAndRefreshCatalog(
            List<ProxyEntity> proxies,
            String selectionLabel,
            int deepProbeLimit
    ) {
        ProxyBatchCheckSummary summary = checkProxies(proxies, selectionLabel, deepProbeLimit);
        if (summary.totalChecked() > 0) {
            publicCatalogCacheService.evictPublicCatalogViews();
        }
        return summary;
    }

    private ProxyBatchCheckSummary checkProxies(
            List<ProxyEntity> proxies,
            String selectionLabel,
            int deepProbeLimit
    ) {
        if (proxies.isEmpty()) {
            log.info("No {} found for checking", selectionLabel);
            return ProxyBatchCheckSummary.empty();
        }

        int effectiveConcurrency = resolveEffectiveConcurrency();
        int submissionWindowSize = resolveSubmissionWindowSize(effectiveConcurrency);

        log.info(
                "Checking {} {} with effectiveConcurrency={} and submissionWindowSize={}",
                proxies.size(),
                selectionLabel,
                effectiveConcurrency,
                submissionWindowSize
        );

        AtomicInteger quickOkCount = new AtomicInteger();
        AtomicInteger verifiedCount = new AtomicInteger();
        AtomicInteger deadCount = new AtomicInteger();
        AtomicInteger deepProbesStarted = new AtomicInteger();

        for (int startIndex = 0; startIndex < proxies.size(); startIndex += submissionWindowSize) {
            int endIndex = Math.min(startIndex + submissionWindowSize, proxies.size());
            List<CompletableFuture<Void>> futures = proxies.subList(startIndex, endIndex).stream()
                    .map(proxy -> CompletableFuture
                            .supplyAsync(() -> checkProxy(proxy, deepProbesStarted, deepProbeLimit), proxyCheckerExecutor)
                            .exceptionally(error -> failedExecution(proxy, error))
                            .thenAccept(execution -> {
                                proxyCheckUpdateService.applyExecution(proxy.getId(), execution);
                                ProxyCheckResult result = execution.finalResult();

                                if (!result.alive()) {
                                    deadCount.incrementAndGet();
                                    proxyMetricsService.incrementCheckFailure();
                                } else if (result.verificationStatus() == ProxyVerificationStatus.VERIFIED) {
                                    verifiedCount.incrementAndGet();
                                    proxyMetricsService.incrementCheckSuccess();
                                } else {
                                    quickOkCount.incrementAndGet();
                                    proxyMetricsService.incrementCheckSuccess();
                                }
                            }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        ProxyBatchCheckSummary summary = new ProxyBatchCheckSummary(
                proxies.size(),
                quickOkCount.get(),
                verifiedCount.get(),
                deadCount.get()
        );

        log.info(
                "Async proxy check completed: total={}, quickOk={}, verified={}, dead={}",
                summary.totalChecked(),
                summary.quickOkCount(),
                summary.verifiedCount(),
                summary.deadCount()
        );

        return summary;
    }

    private ProxyCheckExecution checkProxy(ProxyEntity proxy, AtomicInteger deepProbesStarted, int deepProbeLimit) {
        boolean allowDeepProbe = deepProbesStarted.getAndIncrement() < deepProbeLimit;
        ProxyCheckExecution execution = proxyCheckExecutionService.execute(proxy, allowDeepProbe);
        if (allowDeepProbe && execution.finalResult().failureCode() != null) {
            log.debug(
                    "Deep probe did not verify proxyId={}: code={}, reason={}",
                    proxy.getId(),
                    execution.finalResult().failureCode(),
                    execution.finalResult().errorMessage()
            );
        }
        return execution;
    }

    private ProxyCheckExecution failedExecution(ProxyEntity proxy, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        log.warn("Connectivity check failed for proxyId={}: {}", proxy.getId(), message);

        ProxyCheckResult result = new ProxyCheckResult(
                false,
                -1,
                ProxyVerificationStatus.UNVERIFIED,
                null,
                message
        );
        return new ProxyCheckExecution(
                result,
                List.of(new ProxyCheckHistoryRecord(
                        LocalDateTime.now(),
                        ProxyCheckType.QUICK,
                        result.alive(),
                        result.verificationStatus(),
                        null,
                        result.failureCode(),
                        result.errorMessage()
                ))
        );
    }

    private int resolveEffectiveConcurrency() {
        int configured = checkerExecutorProperties.getEffectiveConcurrency();
        if (configured > 0) {
            return Math.max(1, Math.min(configured, checkerExecutorProperties.getMaxPoolSize()));
        }
        return Math.max(checkerExecutorProperties.getMaxPoolSize(), 1);
    }

    private int resolveSubmissionWindowSize(int effectiveConcurrency) {
        int configured = checkerExecutorProperties.getSubmissionWindowSize();
        if (configured > 0) {
            return Math.max(1, Math.min(configured, effectiveConcurrency));
        }
        return effectiveConcurrency;
    }

}
