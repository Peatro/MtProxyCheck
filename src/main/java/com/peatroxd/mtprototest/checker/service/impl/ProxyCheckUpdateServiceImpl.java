package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckHistoryRecord;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.config.FeedbackProperties;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyFeedbackRepository;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.scoring.model.ProxyScoreContext;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyCheckUpdateServiceImpl implements ProxyCheckUpdateService {

    private final ProxyRepository proxyRepository;
    private final ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    private final ProxyFeedbackRepository proxyFeedbackRepository;
    private final FeedbackProperties feedbackProperties;
    private final ProxyScoringService proxyScoringService;
    private final PublicCatalogCacheService publicCatalogCacheService;

    @Override
    @Transactional
    public void applyExecution(Long proxyId, ProxyCheckExecution execution) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyId));

        saveHistory(proxy, execution.historyRecords());
        applyResult(proxy, execution.finalResult(), latestCheckedAt(execution.historyRecords()));
        applyScore(proxy);

        proxyRepository.save(proxy);
        publicCatalogCacheService.evictProxyById(proxyId);
    }

    @Override
    @Transactional
    public void applyE2eResult(Long proxyId, boolean alive, Long latencyMs, String error) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyId));

        LocalDateTime now = LocalDateTime.now();
        applyE2eTransition(proxy, alive, latencyMs, now);

        saveHistory(proxy, List.of(new ProxyCheckHistoryRecord(
                now,
                ProxyCheckType.E2E,
                alive,
                proxy.getVerificationStatus(),
                alive && latencyMs != null && latencyMs >= 0 ? latencyMs : null,
                null,
                alive ? null : error
        )));
        applyScore(proxy);

        proxyRepository.save(proxy);
        publicCatalogCacheService.evictProxyById(proxyId);
    }

    private void applyScore(ProxyEntity proxy) {
        proxy.setScore(proxyScoringService.calculateScore(new ProxyScoreContext(
                proxy,
                proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(proxy.getId()),
                proxyFeedbackRepository.findTop20ByProxyIdOrderByCreatedAtDesc(proxy.getId())
                        .stream()
                        .limit(feedbackProperties.getRecentLimit())
                        .toList()
        )));
    }

    private void saveHistory(ProxyEntity proxy, List<ProxyCheckHistoryRecord> historyRecords) {
        if (historyRecords == null || historyRecords.isEmpty()) {
            return;
        }

        proxyCheckHistoryRepository.saveAll(historyRecords.stream()
                .map(record -> ProxyCheckHistoryEntity.builder()
                        .proxy(proxy)
                        .checkedAt(record.checkedAt())
                        .checkType(record.checkType())
                        .alive(record.alive())
                        .verificationStatus(record.verificationStatus())
                        .latencyMs(record.latencyMs())
                        .failureCode(record.failureCode())
                        .failureReason(record.failureReason())
                        .build())
                .toList());
    }

    private void applyResult(ProxyEntity proxy, ProxyCheckResult result, LocalDateTime checkedAt) {
        proxy.setLastCheckedAt(checkedAt);

        if (result.alive()) {
            proxy.setStatus(ProxyStatus.ALIVE);
            proxy.setLastLatencyMs(result.latencyMs());
            proxy.setVerificationStatus(resolveDigestVerification(proxy, result));
            proxy.setLastSuccessAt(checkedAt);
            proxy.setConsecutiveSuccesses(safeInt(proxy.getConsecutiveSuccesses()) + 1);
            proxy.setConsecutiveFailures(0);
            return;
        }

        proxy.setStatus(ProxyStatus.DEAD);
        proxy.setLastLatencyMs(null);
        proxy.setVerificationStatus(ProxyVerificationStatus.UNVERIFIED);
        proxy.setConsecutiveFailures(safeInt(proxy.getConsecutiveFailures()) + 1);
        proxy.setConsecutiveSuccesses(0);
    }

    /**
     * Verification при живом (alive) digest-результате:
     * - digest прошёл (PROTOCOL_OK): поднимаем до PROTOCOL_OK, но не понижаем уже-VERIFIED;
     * - digest запускался и упал (QUICK_OK + failureCode): понижаем до QUICK_OK, включая VERIFIED;
     * - shallow QUICK-only (deep не запускался, failureCode == null): не понижаем достигнутое.
     */
    private ProxyVerificationStatus resolveDigestVerification(ProxyEntity proxy, ProxyCheckResult result) {
        ProxyVerificationStatus current = proxy.getVerificationStatus();
        ProxyVerificationStatus res = result.verificationStatus();

        if (res == ProxyVerificationStatus.PROTOCOL_OK) {
            return current == ProxyVerificationStatus.VERIFIED
                    ? ProxyVerificationStatus.VERIFIED
                    : ProxyVerificationStatus.PROTOCOL_OK;
        }
        if (res == ProxyVerificationStatus.QUICK_OK && result.failureCode() != null) {
            return ProxyVerificationStatus.QUICK_OK;
        }
        return higherOf(current, res);
    }

    private void applyE2eTransition(ProxyEntity proxy, boolean alive, Long latencyMs, LocalDateTime now) {
        proxy.setLastCheckedAt(now);

        if (alive) {
            // OK: PROTOCOL_OK -> VERIFIED (промоушен); VERIFIED -> VERIFIED (переподтверждён)
            proxy.setVerificationStatus(ProxyVerificationStatus.VERIFIED);
            proxy.setLastSuccessAt(now);
            if (latencyMs != null && latencyMs >= 0) {
                proxy.setLastLatencyMs(latencyMs);
            }
            proxy.setE2eFailureStreak(0);
            return;
        }

        int streak = safeInt(proxy.getE2eFailureStreak()) + 1;
        proxy.setE2eFailureStreak(streak);

        // Пороги через >= + guard по текущему статусу: самокорректируется, если порог «проскочили»
        // (следующий фейл поймает по статусу), и понижает только с ожидаемого статуса.
        if (streak >= 5 && proxy.getVerificationStatus() == ProxyVerificationStatus.PROTOCOL_OK) {
            // устойчиво мёртвый по E2E — вычистка; digest-retry подхватит позже
            proxy.setStatus(ProxyStatus.DEAD);
            proxy.setVerificationStatus(ProxyVerificationStatus.UNVERIFIED);
            proxy.setLastLatencyMs(null);
            proxy.setE2eFailureStreak(0);
        } else if (streak >= 3 && proxy.getVerificationStatus() == ProxyVerificationStatus.VERIFIED) {
            proxy.setVerificationStatus(ProxyVerificationStatus.PROTOCOL_OK);
        }
        // иначе статус держим (буфер от флапа ТСПУ), score ползёт вниз через e2eFailureStreak
    }

    private ProxyVerificationStatus higherOf(ProxyVerificationStatus a, ProxyVerificationStatus b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private LocalDateTime latestCheckedAt(List<ProxyCheckHistoryRecord> historyRecords) {
        return historyRecords == null || historyRecords.isEmpty()
                ? LocalDateTime.now()
                : historyRecords.getLast().checkedAt();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
