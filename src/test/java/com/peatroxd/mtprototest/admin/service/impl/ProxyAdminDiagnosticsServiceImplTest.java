package com.peatroxd.mtprototest.admin.service.impl;

import com.peatroxd.mtprototest.admin.dto.AdminManualRecheckResponse;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.checker.service.ProxyCheckExecutionService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyAdminDiagnosticsServiceImplTest {

    @Mock
    private ProxyRepository proxyRepository;
    @Mock
    private ProxyCheckExecutionService proxyCheckExecutionService;
    @Mock
    private ProxyCheckUpdateService proxyCheckUpdateService;
    @Mock
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    @Mock
    private PublicCatalogCacheService publicCatalogCacheService;

    @Test
    void shouldRecheckProxyAndReturnUpdatedState() {
        ProxyEntity original = proxy(1L, ProxyStatus.ALIVE, ProxyVerificationStatus.QUICK_OK, 150L);
        ProxyEntity updated = proxy(1L, ProxyStatus.ALIVE, ProxyVerificationStatus.VERIFIED, 90L);

        ProxyAdminDiagnosticsServiceImpl service = new ProxyAdminDiagnosticsServiceImpl(
                proxyRepository,
                proxyCheckExecutionService,
                proxyCheckUpdateService,
                proxyCheckHistoryRepository,
                publicCatalogCacheService
        );

        when(proxyRepository.findById(1L)).thenReturn(Optional.of(original), Optional.of(updated));
        when(proxyCheckExecutionService.execute(original, true)).thenReturn(new ProxyCheckExecution(
                new ProxyCheckResult(true, 90L, ProxyVerificationStatus.VERIFIED, null, null),
                List.of()
        ));

        AdminManualRecheckResponse response = service.recheckProxy(1L);

        assertThat(response.proxyId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("ALIVE");
        assertThat(response.verificationStatus()).isEqualTo("VERIFIED");
        assertThat(response.latencyMs()).isEqualTo(90L);
        assertThat(response.failureCode()).isNull();
        verify(proxyCheckUpdateService).applyExecution(1L, new ProxyCheckExecution(
                new ProxyCheckResult(true, 90L, ProxyVerificationStatus.VERIFIED, null, null),
                List.of()
        ));
        verify(publicCatalogCacheService).evictProxyById(1L);
        verify(publicCatalogCacheService).evictPublicCatalogViews();
    }

    @Test
    void shouldExposeFailureDetailsFromManualRecheck() {
        ProxyEntity proxy = proxy(2L, ProxyStatus.ALIVE, ProxyVerificationStatus.QUICK_OK, 140L);

        ProxyAdminDiagnosticsServiceImpl service = new ProxyAdminDiagnosticsServiceImpl(
                proxyRepository,
                proxyCheckExecutionService,
                proxyCheckUpdateService,
                proxyCheckHistoryRepository,
                publicCatalogCacheService
        );

        when(proxyRepository.findById(2L)).thenReturn(Optional.of(proxy), Optional.of(proxy));
        when(proxyCheckExecutionService.execute(proxy, true)).thenReturn(new ProxyCheckExecution(
                new ProxyCheckResult(true, 140L, ProxyVerificationStatus.QUICK_OK, MtProtoProbeFailureCode.CONNECT_ERROR, "Dial timeout"),
                List.of()
        ));

        AdminManualRecheckResponse response = service.recheckProxy(2L);

        assertThat(response.failureCode()).isEqualTo("CONNECT_ERROR");
        assertThat(response.failureReason()).isEqualTo("Dial timeout");
    }

    private ProxyEntity proxy(Long id, ProxyStatus status, ProxyVerificationStatus verificationStatus, Long latencyMs) {
        return ProxyEntity.builder()
                .id(id)
                .host("host" + id)
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(status)
                .verificationStatus(verificationStatus)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(50)
                .lastLatencyMs(latencyMs)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build();
    }
}
