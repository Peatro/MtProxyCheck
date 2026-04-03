package com.peatroxd.mtprototest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ClientRequestKeyResolver clientRequestKeyResolver;
    private final RateLimitProperties rateLimitProperties;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitInterceptor(ClientRequestKeyResolver clientRequestKeyResolver, RateLimitProperties rateLimitProperties) {
        this.clientRequestKeyResolver = clientRequestKeyResolver;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            return true;
        }

        String clientKey = clientRequestKeyResolver.resolve(request);
        String rateKey = rule.keyPrefix() + ":" + clientKey;
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.compute(rateKey, (ignored, existing) -> {
            if (existing == null || now - existing.windowStartedAt >= rule.windowMs()) {
                return new WindowCounter(now, new AtomicInteger(1));
            }

            existing.requestCount.incrementAndGet();
            return existing;
        });

        if (counter.requestCount.get() > rule.limit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }

        return true;
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && path.matches(".*/api/proxies/\\d+/feedback$")) {
            return new RateLimitRule("feedback", rateLimitProperties.getFeedbackWriteLimit(), rateLimitProperties.getFeedbackWriteWindowMs());
        }

        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/proxies")) {
            return new RateLimitRule("public-read", rateLimitProperties.getPublicReadLimit(), rateLimitProperties.getPublicReadWindowMs());
        }

        return null;
    }

    private record RateLimitRule(String keyPrefix, int limit, long windowMs) {
    }

    private static final class WindowCounter {
        private final long windowStartedAt;
        private final AtomicInteger requestCount;

        private WindowCounter(long windowStartedAt, AtomicInteger requestCount) {
            this.windowStartedAt = windowStartedAt;
            this.requestCount = requestCount;
        }
    }
}
