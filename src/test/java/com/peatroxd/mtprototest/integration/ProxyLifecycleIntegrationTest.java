package com.peatroxd.mtprototest.integration;

import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckHistoryRecord;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProxyLifecycleIntegrationTest {

    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    @Autowired
    private ProxyCheckUpdateService proxyCheckUpdateService;
    @Autowired
    private ProxyService proxyService;
    @Autowired
    private ProxyFeedbackService proxyFeedbackService;

    @Test
    void shouldPersistCheckHistoryUpdateLifecycleAndExposeStats() {
        ProxyEntity proxy = proxyRepository.save(ProxyEntity.builder()
                .host("1.1.1.1")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("integration")
                .status(ProxyStatus.NEW)
                .verificationStatus(ProxyVerificationStatus.UNVERIFIED)
                .score(0)
                .consecutiveFailures(0)
                .consecutiveSuccesses(0)
                .build());

        LocalDateTime quickAt = LocalDateTime.now().minusSeconds(5);
        LocalDateTime deepAt = LocalDateTime.now();
        proxyCheckUpdateService.applyExecution(proxy.getId(), new ProxyCheckExecution(
                new ProxyCheckResult(true, 120, ProxyVerificationStatus.VERIFIED, null, null),
                List.of(
                        new ProxyCheckHistoryRecord(quickAt, ProxyCheckType.QUICK, true, ProxyVerificationStatus.QUICK_OK, 140L, null, null),
                        new ProxyCheckHistoryRecord(deepAt, ProxyCheckType.DEEP, true, ProxyVerificationStatus.VERIFIED, 120L, null, null)
                )
        ));

        ProxyEntity updated = proxyRepository.findById(proxy.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProxyStatus.ALIVE);
        assertThat(updated.getVerificationStatus()).isEqualTo(ProxyVerificationStatus.VERIFIED);
        assertThat(updated.getLastCheckedAt()).isEqualTo(deepAt);
        assertThat(updated.getConsecutiveSuccesses()).isEqualTo(1);
        assertThat(proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(proxy.getId())).hasSize(2);

        proxyFeedbackService.submitFeedback(
                proxy.getId(),
                new ProxyFeedbackRequest(ProxyFeedbackResult.WORKED, ProxyFeedbackPlatform.DESKTOP),
                "127.0.0.1"
        );

        assertThatThrownBy(() -> proxyFeedbackService.submitFeedback(
                proxy.getId(),
                new ProxyFeedbackRequest(ProxyFeedbackResult.WORKED, ProxyFeedbackPlatform.DESKTOP),
                "127.0.0.1"
        )).isInstanceOf(ResponseStatusException.class);

        var stats = proxyService.getStats();
        assertThat(stats.totalProxies()).isGreaterThanOrEqualTo(1);
        assertThat(stats.aliveCount()).isGreaterThanOrEqualTo(1);
        assertThat(stats.verifiedCount()).isGreaterThanOrEqualTo(1);

        var lifecycleBatch = proxyRepository.findLifecycleBatchByStatusAndVerificationStatus(
                ProxyStatus.ALIVE,
                ProxyVerificationStatus.VERIFIED,
                LocalDateTime.now().plusMinutes(1),
                PageRequest.of(0, 10)
        );
        assertThat(lifecycleBatch).extracting(ProxyEntity::getId).contains(updated.getId());
    }
}
