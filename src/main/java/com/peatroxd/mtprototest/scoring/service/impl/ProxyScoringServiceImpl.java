package com.peatroxd.mtprototest.scoring.service.impl;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.scoring.model.ProxyScoreContext;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProxyScoringServiceImpl implements ProxyScoringService {

    @Override
    public int calculateScore(ProxyScoreContext context) {
        if (context == null || context.proxy() == null || context.proxy().getStatus() != ProxyStatus.ALIVE) {
            return 0;
        }

        List<ProxyCheckHistoryEntity> recentChecks = context.recentChecks() != null ? context.recentChecks() : List.of();

        int verificationWeight = switch (context.proxy().getVerificationStatus()) {
            case VERIFIED -> 28;
            case QUICK_OK -> 20;
            case UNVERIFIED -> 6;
        };

        long totalChecks = recentChecks.size();
        long successfulChecks = recentChecks.stream()
                .filter(ProxyCheckHistoryEntity::isAlive)
                .count();
        int successRateScore = totalChecks == 0
                ? 12
                : (int) Math.round((successfulChecks * 35.0d) / totalChecks);

        double averageLatency = recentChecks.stream()
                .filter(ProxyCheckHistoryEntity::isAlive)
                .map(ProxyCheckHistoryEntity::getLatencyMs)
                .filter(latency -> latency != null && latency > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(context.proxy().getLastLatencyMs() != null ? context.proxy().getLastLatencyMs() : 1_500);

        int latencyScore = Math.max(0, 20 - (int) Math.min(20, averageLatency / 50.0d));

        int freshnessScore = calculateFreshnessScore(context.proxy().getLastSuccessAt());
        int failurePenalty = Math.min(20, safeInt(context.proxy().getConsecutiveFailures()) * 4);
        int feedbackAdjustment = calculateFeedbackAdjustment(context);

        int rawScore = verificationWeight + successRateScore + latencyScore + freshnessScore + feedbackAdjustment - failurePenalty;
        return Math.max(0, Math.min(100, rawScore));
    }

    private int calculateFreshnessScore(LocalDateTime lastSuccessAt) {
        if (lastSuccessAt == null) {
            return 0;
        }

        long minutes = Duration.between(lastSuccessAt, LocalDateTime.now()).toMinutes();
        if (minutes <= 30) {
            return 12;
        }
        if (minutes <= 6 * 60) {
            return 9;
        }
        if (minutes <= 24 * 60) {
            return 6;
        }
        if (minutes <= 3 * 24 * 60) {
            return 3;
        }
        return 0;
    }

    private int calculateFeedbackAdjustment(ProxyScoreContext context) {
        if (context.recentFeedback() == null || context.recentFeedback().isEmpty()) {
            return 0;
        }

        long worked = context.recentFeedback().stream()
                .filter(feedback -> feedback.getResult() == ProxyFeedbackResult.WORKED)
                .count();
        long failed = context.recentFeedback().stream()
                .filter(feedback -> feedback.getResult() == ProxyFeedbackResult.FAILED)
                .count();
        return (int) Math.max(-5, Math.min(5, worked - failed));
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
