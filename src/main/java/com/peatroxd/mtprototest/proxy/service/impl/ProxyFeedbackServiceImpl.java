package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.repository.ProxyFeedbackRepository;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProxyFeedbackServiceImpl implements ProxyFeedbackService {

    private static final int WORKED_SCORE_BONUS = 5;
    private static final int FAILED_SCORE_PENALTY = 8;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 120;

    private final ProxyRepository proxyRepository;
    private final ProxyFeedbackRepository proxyFeedbackRepository;

    @Override
    @Transactional
    public ProxyFeedbackResponse submitFeedback(Long proxyId, ProxyFeedbackRequest request) {
        if (request == null || request.result() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback result is required");
        }

        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proxy not found"));

        ProxyFeedbackPlatform platform = request.platform() != null
                ? request.platform()
                : ProxyFeedbackPlatform.UNKNOWN;

        ProxyFeedbackEntity feedback = ProxyFeedbackEntity.builder()
                .proxy(proxy)
                .result(request.result())
                .platform(platform)
                .build();

        proxyFeedbackRepository.save(feedback);
        applyScoreAdjustment(proxy, request.result());
        proxyRepository.save(proxy);

        return new ProxyFeedbackResponse(
                true,
                proxy.getId(),
                request.result().name(),
                platform.name()
        );
    }

    private void applyScoreAdjustment(ProxyEntity proxy, ProxyFeedbackResult result) {
        int currentScore = proxy.getScore() != null ? proxy.getScore() : 0;
        int adjustedScore = result == ProxyFeedbackResult.WORKED
                ? currentScore + WORKED_SCORE_BONUS
                : currentScore - FAILED_SCORE_PENALTY;

        proxy.setScore(Math.max(MIN_SCORE, Math.min(MAX_SCORE, adjustedScore)));
    }
}
