package com.peatroxd.mtprototest.admin.service.impl;

import com.peatroxd.mtprototest.admin.dto.AdminCatalogOverviewResponse;
import com.peatroxd.mtprototest.admin.dto.AdminProxyModerationResponse;
import com.peatroxd.mtprototest.admin.dto.AdminSourceOverviewResponse;
import com.peatroxd.mtprototest.admin.service.ProxyAdminService;
import com.peatroxd.mtprototest.admin.service.ProxyImportTrackingService;
import com.peatroxd.mtprototest.admin.service.SourceImportSnapshot;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProxyAdminServiceImpl implements ProxyAdminService {

    private final ProxyRepository proxyRepository;
    private final ProxyImportTrackingService proxyImportTrackingService;
    private final PublicCatalogCacheService publicCatalogCacheService;

    public ProxyAdminServiceImpl(
            ProxyRepository proxyRepository,
            ProxyImportTrackingService proxyImportTrackingService,
            PublicCatalogCacheService publicCatalogCacheService
    ) {
        this.proxyRepository = proxyRepository;
        this.proxyImportTrackingService = proxyImportTrackingService;
        this.publicCatalogCacheService = publicCatalogCacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCatalogOverviewResponse getOverview() {
        List<ProxyEntity> proxies = proxyRepository.findAll();
        Map<String, List<ProxyEntity>> bySource = proxies.stream()
                .collect(Collectors.groupingBy(ProxyEntity::getSource));
        Map<String, SourceImportSnapshot> snapshots = proxyImportTrackingService.getSnapshots().stream()
                .collect(Collectors.toMap(SourceImportSnapshot::source, snapshot -> snapshot));

        List<AdminSourceOverviewResponse> sources = Stream.concat(bySource.keySet().stream(), snapshots.keySet().stream())
                .distinct()
                .sorted()
                .map(source -> toSourceOverview(source, bySource.getOrDefault(source, List.of()), snapshots.get(source)))
                .toList();

        return new AdminCatalogOverviewResponse(
                proxies.size(),
                countByStatus(proxies, ProxyStatus.NEW),
                countByStatus(proxies, ProxyStatus.ALIVE),
                countByStatus(proxies, ProxyStatus.DEAD),
                countByStatus(proxies, ProxyStatus.ARCHIVED),
                countByVerificationStatus(proxies, ProxyVerificationStatus.VERIFIED),
                countByVerificationStatus(proxies, ProxyVerificationStatus.QUICK_OK),
                countByVerificationStatus(proxies, ProxyVerificationStatus.UNVERIFIED),
                countByModerationStatus(proxies, ProxyModerationStatus.WHITELISTED),
                countByModerationStatus(proxies, ProxyModerationStatus.BLACKLISTED),
                sources
        );
    }

    @Override
    @Transactional
    public AdminProxyModerationResponse updateModerationStatus(Long proxyId, ProxyModerationStatus moderationStatus) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proxy not found"));
        proxy.setModerationStatus(moderationStatus);
        proxyRepository.save(proxy);
        publicCatalogCacheService.evictProxyById(proxyId);
        publicCatalogCacheService.evictPublicCatalogViews();
        return new AdminProxyModerationResponse(proxyId, moderationStatus.name());
    }

    private AdminSourceOverviewResponse toSourceOverview(
            String source,
            List<ProxyEntity> proxies,
            SourceImportSnapshot snapshot
    ) {
        return new AdminSourceOverviewResponse(
                source,
                proxies.size(),
                countByStatus(proxies, ProxyStatus.ALIVE),
                countByVerificationStatus(proxies, ProxyVerificationStatus.VERIFIED),
                countByModerationStatus(proxies, ProxyModerationStatus.BLACKLISTED),
                snapshot != null ? snapshot.startedAt() : null,
                snapshot != null ? snapshot.completedAt() : null,
                snapshot != null && snapshot.succeeded(),
                snapshot != null ? snapshot.imported() : 0,
                snapshot != null ? snapshot.skipped() : 0,
                snapshot != null ? snapshot.rejected() : 0,
                snapshot != null ? snapshot.errorMessage() : null
        );
    }

    private long countByStatus(List<ProxyEntity> proxies, ProxyStatus status) {
        return proxies.stream().filter(proxy -> proxy.getStatus() == status).count();
    }

    private long countByVerificationStatus(List<ProxyEntity> proxies, ProxyVerificationStatus verificationStatus) {
        return proxies.stream().filter(proxy -> proxy.getVerificationStatus() == verificationStatus).count();
    }

    private long countByModerationStatus(List<ProxyEntity> proxies, ProxyModerationStatus moderationStatus) {
        return proxies.stream().filter(proxy -> proxy.getModerationStatus() == moderationStatus).count();
    }
}
