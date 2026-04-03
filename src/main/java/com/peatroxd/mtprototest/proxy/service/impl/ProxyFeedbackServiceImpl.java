package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.common.cache.PublicCatalogCacheService;
import com.peatroxd.mtprototest.proxy.config.FeedbackProperties;
import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyFeedbackRepository;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.scoring.model.ProxyScoreContext;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(FeedbackProperties.class)
public class ProxyFeedbackServiceImpl implements ProxyFeedbackService {

    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String ANONYMOUS_CLIENT_VALUE = "anonymous";

    private final ProxyRepository proxyRepository;
    private final ProxyFeedbackRepository proxyFeedbackRepository;
    private final ProxyCheckHistoryRepository proxyCheckHistoryRepository;
    private final ProxyScoringService proxyScoringService;
    private final FeedbackProperties feedbackProperties;
    private final PublicCatalogCacheService publicCatalogCacheService;

    @Override
    @Transactional
    public ProxyFeedbackResponse submitFeedback(Long proxyId, ProxyFeedbackRequest request, String clientKey) {
        if (request == null || request.result() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback result is required");
        }

        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proxy not found"));
        if (proxy.getModerationStatus() == ProxyModerationStatus.BLACKLISTED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proxy not found");
        }

        ProxyFeedbackPlatform platform = request.platform() != null ? request.platform() : ProxyFeedbackPlatform.UNKNOWN;
        boolean anonymousClient = clientKey == null || clientKey.isBlank();
        String normalizedClientKey = normalizeClientKey(clientKey);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        long windowMinutes = Math.max(1, feedbackProperties.getDedupeWindowMinutes());
        LocalDateTime hourStart = now.withMinute(0);
        LocalDateTime windowStartedAt = hourStart.plusMinutes((now.getMinute() / windowMinutes) * windowMinutes);
        enforceSubmissionLimit(normalizedClientKey, anonymousClient, now, windowMinutes);

        if (proxyFeedbackRepository.existsByProxyIdAndPlatformAndClientKeyAndWindowStartedAt(
                proxyId,
                platform,
                normalizedClientKey,
                windowStartedAt
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted for this proxy in the current time window");
        }

        ProxyFeedbackEntity feedback = ProxyFeedbackEntity.builder()
                .proxy(proxy)
                .result(request.result())
                .platform(platform)
                .clientKey(normalizedClientKey)
                .windowStartedAt(windowStartedAt)
                .build();

        try {
            proxyFeedbackRepository.save(feedback);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted for this proxy in the current time window");
        }

        proxy.setScore(proxyScoringService.calculateScore(new ProxyScoreContext(
                proxy,
                proxyCheckHistoryRepository.findTop20ByProxyIdOrderByCheckedAtDesc(proxyId),
                proxyFeedbackRepository.findTop20ByProxyIdOrderByCreatedAtDesc(proxyId)
                        .stream()
                        .limit(feedbackProperties.getRecentLimit())
                        .toList()
        )));
        proxyRepository.save(proxy);
        publicCatalogCacheService.evictProxyById(proxyId);
        publicCatalogCacheService.evictPublicCatalogViews();

        return new ProxyFeedbackResponse(true, proxy.getId(), request.result().name(), platform.name());
    }

    private void enforceSubmissionLimit(String normalizedClientKey, boolean anonymousClient, LocalDateTime now, long windowMinutes) {
        LocalDateTime windowStart = now.minusMinutes(windowMinutes);
        long submissionsInWindow = proxyFeedbackRepository.countByClientKeyAndCreatedAtAfter(normalizedClientKey, windowStart);
        int maxAllowed = anonymousClient
                ? feedbackProperties.getAnonymousMaxSubmissionsPerWindow()
                : feedbackProperties.getMaxSubmissionsPerWindow();

        if (submissionsInWindow >= maxAllowed) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Feedback submission limit exceeded");
        }
    }

    private String normalizeClientKey(String clientKey) {
        String value = (clientKey == null || clientKey.isBlank()) ? ANONYMOUS_CLIENT_VALUE : clientKey.trim();
        try {
            return HEX_FORMAT.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process feedback fingerprint");
        }
    }
}
