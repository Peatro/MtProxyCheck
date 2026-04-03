package com.peatroxd.mtprototest.admin.service.impl;

import com.peatroxd.mtprototest.admin.dto.AdminDeepProbeFailureItemResponse;
import com.peatroxd.mtprototest.admin.dto.AdminDeepProbeFailureSummaryResponse;
import com.peatroxd.mtprototest.admin.dto.AdminDeepProbeFailuresResponse;
import com.peatroxd.mtprototest.admin.dto.AdminManualRecheckResponse;
import com.peatroxd.mtprototest.admin.service.ProxyAdminDiagnosticsService;
import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.checker.service.ProxyCheckExecutionService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProxyAdminDiagnosticsServiceImpl implements ProxyAdminDiagnosticsService {

    private final ProxyRepository proxyRepository;
    private final ProxyCheckExecutionService proxyCheckExecutionService;
    private final ProxyCheckUpdateService proxyCheckUpdateService;
    private final ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    private final PublicCatalogCacheService publicCatalogCacheService;

    public ProxyAdminDiagnosticsServiceImpl(
            ProxyRepository proxyRepository,
            ProxyCheckExecutionService proxyCheckExecutionService,
            ProxyCheckUpdateService proxyCheckUpdateService,
            ProxyCheckHistoryRepository proxyCheckHistoryRepository,
            PublicCatalogCacheService publicCatalogCacheService
    ) {
        this.proxyRepository = proxyRepository;
        this.proxyCheckExecutionService = proxyCheckExecutionService;
        this.proxyCheckUpdateService = proxyCheckUpdateService;
        this.proxyCheckHistoryRepository = proxyCheckHistoryRepository;
        this.publicCatalogCacheService = publicCatalogCacheService;
    }

    @Override
    @Transactional
    public AdminManualRecheckResponse recheckProxy(Long proxyId) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proxy not found"));

        ProxyCheckExecution execution = proxyCheckExecutionService.execute(proxy, true);
        proxyCheckUpdateService.applyExecution(proxyId, execution);
        publicCatalogCacheService.evictProxyById(proxyId);
        publicCatalogCacheService.evictPublicCatalogViews();

        ProxyEntity updated = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proxy not found"));

        return new AdminManualRecheckResponse(
                updated.getId(),
                updated.getStatus().name(),
                updated.getVerificationStatus().name(),
                updated.getLastLatencyMs(),
                execution.finalResult().failureCode() != null ? execution.finalResult().failureCode().name() : null,
                execution.finalResult().errorMessage()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDeepProbeFailuresResponse getDeepProbeFailures() {
        List<AdminDeepProbeFailureSummaryResponse> summary = Arrays.stream(MtProtoProbeFailureCode.values())
                .map(code -> new AdminDeepProbeFailureSummaryResponse(
                        code.name(),
                        proxyCheckHistoryRepository.countByCheckTypeAndFailureCode(ProxyCheckType.DEEP, code)
                ))
                .filter(item -> item.count() > 0)
                .toList();

        List<AdminDeepProbeFailureItemResponse> recent = proxyCheckHistoryRepository
                .findTop50ByCheckTypeAndFailureCodeIsNotNullOrderByCheckedAtDesc(ProxyCheckType.DEEP)
                .stream()
                .map(this::toFailureItem)
                .toList();

        return new AdminDeepProbeFailuresResponse(summary, recent);
    }

    private AdminDeepProbeFailureItemResponse toFailureItem(ProxyCheckHistoryEntity entity) {
        ProxyEntity proxy = entity.getProxy();
        return new AdminDeepProbeFailureItemResponse(
                proxy.getId(),
                proxy.getHost(),
                proxy.getPort(),
                proxy.getSource(),
                entity.getFailureCode() != null ? entity.getFailureCode().name() : null,
                entity.getFailureReason(),
                entity.getCheckedAt()
        );
    }
}
