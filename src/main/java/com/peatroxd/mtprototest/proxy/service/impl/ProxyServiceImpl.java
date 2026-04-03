package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheNames;
import com.peatroxd.mtprototest.proxy.dto.request.ProxyListRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyPageResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyStatsResponse;
import com.peatroxd.mtprototest.proxy.dto.response.RecentCheckSummaryResponse;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.mapper.ProxyResponseMapper;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProxyServiceImpl implements ProxyService {

    private static final int BEST_PROXY_LIMIT = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "host",
            "port",
            "status",
            "verificationStatus",
            "score",
            "lastLatencyMs",
            "createdAt",
            "updatedAt",
            "lastCheckedAt",
            "lastSuccessAt",
            "consecutiveFailures",
            "consecutiveSuccesses"
    );

    private final ProxyRepository proxyRepository;
    private final ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    private final ProxyResponseMapper proxyResponseMapper;

    @Override
    @Cacheable(PublicCatalogCacheNames.PROXY_BEST)
    public List<ProxyResponse> getBest() {
        return proxyRepository.findTop200ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus.ALIVE)
                .stream()
                .filter(this::isPubliclyVisible)
                .sorted(bestProxyComparator())
                .limit(BEST_PROXY_LIMIT)
                .map(proxyResponseMapper::toResponse)
                .toList();
    }

    @Override
    public ProxyPageResponse getProxies(ProxyListRequest request) {
        Sort sort = sanitizeSort(request.sortBy(), request.sortDirection());
        PageRequest pageRequest = PageRequest.of(request.page(), request.size(), sort);
        Page<ProxyEntity> page = proxyRepository.findAll(buildSpecification(request), pageRequest);

        return new ProxyPageResponse(
                page.getContent().stream().map(proxyResponseMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Cacheable(cacheNames = PublicCatalogCacheNames.PROXY_BY_ID, key = "#proxyId")
    public ProxyResponse getById(Long proxyId) {
        return proxyRepository.findById(proxyId)
                .filter(this::isPubliclyVisible)
                .map(proxyResponseMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proxy not found"));
    }

    @Override
    @Cacheable(PublicCatalogCacheNames.PROXY_STATS)
    public ProxyStatsResponse getStats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Specification<ProxyEntity> publicVisible = publiclyVisible();

        return new ProxyStatsResponse(
                proxyRepository.count(publicVisible),
                proxyRepository.count(publicVisible.and(equalsStatus(ProxyStatus.NEW))),
                proxyRepository.count(publicVisible.and(equalsStatus(ProxyStatus.ALIVE))),
                proxyRepository.count(publicVisible.and(equalsStatus(ProxyStatus.DEAD))),
                proxyRepository.count(publicVisible.and(equalsVerificationStatus(ProxyVerificationStatus.VERIFIED))),
                proxyRepository.count(publicVisible.and(equalsVerificationStatus(ProxyVerificationStatus.QUICK_OK))),
                proxyRepository.count(publicVisible.and(equalsVerificationStatus(ProxyVerificationStatus.UNVERIFIED))),
                new RecentCheckSummaryResponse(
                        proxyCheckHistoryRepository.countByCheckedAtAfter(since),
                        proxyCheckHistoryRepository.countByCheckedAtAfterAndAliveIsTrue(since),
                        proxyCheckHistoryRepository.countByCheckedAtAfterAndAliveIsFalse(since),
                        proxyCheckHistoryRepository.countByCheckedAtAfterAndCheckTypeAndAliveIsTrue(since, ProxyCheckType.DEEP),
                        proxyCheckHistoryRepository.countByCheckedAtAfterAndCheckTypeAndAliveIsFalse(since, ProxyCheckType.DEEP)
                )
        );
    }

    private Specification<ProxyEntity> buildSpecification(ProxyListRequest request) {
        return Specification.<ProxyEntity>allOf()
                .and(publiclyVisible())
                .and(equalsStatus(request.status()))
                .and(equalsVerificationStatus(request.verificationStatus()))
                .and(minScore(request.minScore()))
                .and(maxLatency(request.maxLatency()));
    }

    private Specification<ProxyEntity> publiclyVisible() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("moderationStatus"), ProxyModerationStatus.BLACKLISTED);
    }

    private Specification<ProxyEntity> equalsStatus(ProxyStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<ProxyEntity> equalsVerificationStatus(ProxyVerificationStatus verificationStatus) {
        return (root, query, criteriaBuilder) ->
                verificationStatus == null ? null : criteriaBuilder.equal(root.get("verificationStatus"), verificationStatus);
    }

    private Specification<ProxyEntity> minScore(Integer minScore) {
        return (root, query, criteriaBuilder) ->
                minScore == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("score"), minScore);
    }

    private Specification<ProxyEntity> maxLatency(Long maxLatency) {
        return (root, query, criteriaBuilder) ->
                maxLatency == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("lastLatencyMs"), maxLatency);
    }

    private Sort sanitizeSort(String sortBy, String sortDirection) {
        String field = (sortBy == null || sortBy.isBlank()) ? "score" : sortBy;
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported sortBy: " + field);
        }

        Sort.Direction direction;
        try {
            direction = sortDirection == null || sortDirection.isBlank()
                    ? Sort.Direction.DESC
                    : Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported sortDirection: " + sortDirection);
        }

        return Sort.by(direction, field);
    }

    private Comparator<ProxyEntity> bestProxyComparator() {
        return Comparator
                .comparingInt(this::moderationRank)
                .thenComparingInt(this::verificationRank)
                .thenComparing(ProxyEntity::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(proxy -> proxy.getLastSuccessAt() == null ? LocalDateTime.MIN : proxy.getLastSuccessAt(), Comparator.reverseOrder())
                .thenComparing(proxy -> proxy.getLastLatencyMs() == null ? Long.MAX_VALUE : proxy.getLastLatencyMs())
                .thenComparing(ProxyEntity::getId);
    }

    private boolean isPubliclyVisible(ProxyEntity proxy) {
        return proxy.getModerationStatus() != ProxyModerationStatus.BLACKLISTED;
    }

    private int moderationRank(ProxyEntity proxy) {
        ProxyModerationStatus moderationStatus = proxy.getModerationStatus() != null
                ? proxy.getModerationStatus()
                : ProxyModerationStatus.NORMAL;
        return switch (moderationStatus) {
            case WHITELISTED -> 0;
            case NORMAL -> 1;
            case BLACKLISTED -> 2;
        };
    }

    private int verificationRank(ProxyEntity proxy) {
        return switch (proxy.getVerificationStatus()) {
            case VERIFIED -> 0;
            case QUICK_OK -> 1;
            case UNVERIFIED -> 2;
        };
    }
}
