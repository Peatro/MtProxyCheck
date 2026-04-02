package com.peatroxd.mtprototest.scoring.service.impl;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.scoring.model.ProxyScoreContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyScoringServiceImplTest {

    private final ProxyScoringServiceImpl service = new ProxyScoringServiceImpl();

    @Test
    void shouldGiveHigherScoreToHealthyVerifiedProxy() {
        ProxyEntity healthy = proxy(ProxyStatus.ALIVE, ProxyVerificationStatus.VERIFIED, 0, LocalDateTime.now().minusMinutes(5), 120L);
        ProxyEntity weak = proxy(ProxyStatus.ALIVE, ProxyVerificationStatus.QUICK_OK, 3, LocalDateTime.now().minusDays(4), 900L);

        int healthyScore = service.calculateScore(new ProxyScoreContext(
                healthy,
                List.of(history(true, 110L), history(true, 130L), history(true, 90L)),
                List.of(feedback(ProxyFeedbackResult.WORKED))
        ));
        int weakScore = service.calculateScore(new ProxyScoreContext(
                weak,
                List.of(history(true, 900L), history(false, null), history(false, null)),
                List.of(feedback(ProxyFeedbackResult.FAILED))
        ));

        assertThat(healthyScore).isGreaterThan(weakScore);
        assertThat(healthyScore).isBetween(0, 100);
        assertThat(weakScore).isBetween(0, 100);
    }

    @Test
    void shouldReturnZeroForDeadProxy() {
        ProxyEntity dead = proxy(ProxyStatus.DEAD, ProxyVerificationStatus.UNVERIFIED, 5, null, null);

        assertThat(service.calculateScore(new ProxyScoreContext(dead, List.of(), List.of()))).isZero();
    }

    private ProxyEntity proxy(
            ProxyStatus status,
            ProxyVerificationStatus verificationStatus,
            int consecutiveFailures,
            LocalDateTime lastSuccessAt,
            Long lastLatencyMs
    ) {
        return ProxyEntity.builder()
                .id(1L)
                .host("host")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(status)
                .verificationStatus(verificationStatus)
                .consecutiveFailures(consecutiveFailures)
                .consecutiveSuccesses(0)
                .lastSuccessAt(lastSuccessAt)
                .lastLatencyMs(lastLatencyMs)
                .score(0)
                .build();
    }

    private ProxyCheckHistoryEntity history(boolean alive, Long latencyMs) {
        return ProxyCheckHistoryEntity.builder()
                .alive(alive)
                .verificationStatus(alive ? ProxyVerificationStatus.QUICK_OK : ProxyVerificationStatus.UNVERIFIED)
                .latencyMs(latencyMs)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    private ProxyFeedbackEntity feedback(ProxyFeedbackResult result) {
        return ProxyFeedbackEntity.builder()
                .result(result)
                .platform(ProxyFeedbackPlatform.UNKNOWN)
                .createdAt(LocalDateTime.now())
                .clientKey("client")
                .windowStartedAt(LocalDateTime.now())
                .build();
    }
}
