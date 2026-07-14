package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.client.ProxyPingAgentClient;
import com.peatroxd.mtprototest.checker.client.ProxyPingAgentClient.PingResult;
import com.peatroxd.mtprototest.checker.config.TdlibProperties;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Развязанный шедулер end-to-end проверки прокси через внешний TDLib-агент.
 * Не входит в digest-батч checker'а: отдельный интервал, отдельный пул, отдельная ось статусов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TdlibVerificationScheduler {

    private final ProxyRepository proxyRepository;
    private final ProxyPingAgentClient agentClient;
    private final ProxyCheckUpdateService proxyCheckUpdateService;
    private final ProxyMetricsService proxyMetricsService;
    private final TdlibProperties properties;
    private final Executor tdlibProbeExecutor;

    @Scheduled(
            initialDelayString = "${app.checker.tdlib.interval:60000}",
            fixedDelayString = "${app.checker.tdlib.interval:60000}"
    )
    public void verifyEndToEnd() {
        if (!agentClient.healthy()) {
            log.warn("TDLib agent unreachable, skipping E2E verification cycle");
            return;
        }

        List<ProxyEntity> candidates = selectCandidates();
        if (candidates.isEmpty()) {
            log.debug("No E2E candidates to verify");
            return;
        }

        log.info("TDLib E2E verification: {} candidates, concurrency={}", candidates.size(), properties.getConcurrency());

        List<CompletableFuture<Void>> futures = candidates.stream()
                .map(proxy -> CompletableFuture.runAsync(() -> verifyProxy(proxy), tdlibProbeExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void verifyProxy(ProxyEntity proxy) {
        PingResult result = agentClient.probe(proxy.getHost(), proxy.getPort(), proxy.getSecret());
        Long latencyMs = result.alive() && result.seconds() != null
                ? Math.round(result.seconds() * 1000)
                : null;
        proxyCheckUpdateService.applyE2eResult(proxy.getId(), result.alive(), latencyMs, result.error());
        proxyMetricsService.incrementE2eCheck(result.alive());
    }

    /**
     * Справедливый сплит пополам: половина лимита — PROTOCOL_OK (промоушен),
     * половина — VERIFIED старше recheck-after. При нехватке одной категории
     * вторая добирает освободившиеся слоты (не простаивать).
     */
    private List<ProxyEntity> selectCandidates() {
        int limit = properties.getBatchLimit();
        int promoTarget = limit / 2;
        int recheckTarget = limit - promoTarget;

        LocalDateTime recheckBefore = LocalDateTime.now().minusNanos(properties.getRecheckAfterMs() * 1_000_000);
        List<ProxyEntity> promo = proxyRepository.findE2ePromotionCandidates(PageRequest.of(0, limit));
        List<ProxyEntity> recheck = proxyRepository.findE2eRecheckCandidates(recheckBefore, PageRequest.of(0, limit));

        int nPromo = Math.min(promo.size(), promoTarget);
        int nRecheck = Math.min(recheck.size(), recheckTarget);

        int free = limit - nPromo - nRecheck;
        if (free > 0) {
            int addPromo = Math.min(free, promo.size() - nPromo);
            nPromo += addPromo;
            free -= addPromo;
        }
        if (free > 0) {
            nRecheck += Math.min(free, recheck.size() - nRecheck);
        }

        List<ProxyEntity> selected = new ArrayList<>(nPromo + nRecheck);
        selected.addAll(promo.subList(0, nPromo));
        selected.addAll(recheck.subList(0, nRecheck));
        return selected;
    }
}
