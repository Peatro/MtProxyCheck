package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckHistoryRecord;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
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

    @Override
    @Transactional
    public void applyExecution(Long proxyId, ProxyCheckExecution execution) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyId));

        saveHistory(proxy, execution.historyRecords());
        applyResult(proxy, execution.finalResult(), latestCheckedAt(execution.historyRecords()));
        proxy.setScore(proxyScoringService.calculateScore(new ProxyScoreContext(
                proxy,
                proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(proxyId),
                proxyFeedbackRepository.findTop20ByProxyIdOrderByCreatedAtDesc(proxyId)
                        .stream()
                        .limit(feedbackProperties.getRecentLimit())
                        .toList()
        )));

        proxyRepository.save(proxy);
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
            proxy.setVerificationStatus(result.verificationStatus());
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

    private LocalDateTime latestCheckedAt(List<ProxyCheckHistoryRecord> historyRecords) {
        return historyRecords == null || historyRecords.isEmpty()
                ? LocalDateTime.now()
                : historyRecords.getLast().checkedAt();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
