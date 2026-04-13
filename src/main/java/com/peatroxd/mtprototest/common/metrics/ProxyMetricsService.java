package com.peatroxd.mtprototest.common.metrics;

import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.parser.model.RawProxyRejectReason;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ProxyMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter importedCounter;
    private final Counter checkSuccessCounter;
    private final Counter checkFailureCounter;

    public ProxyMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.importedCounter = meterRegistry.counter("proxy.imported.total");
        this.checkSuccessCounter = meterRegistry.counter("proxy.check.success.total");
        this.checkFailureCounter = meterRegistry.counter("proxy.check.failure.total");
    }

    public void incrementImported(int importedCount) {
        if (importedCount > 0) {
            importedCounter.increment(importedCount);
        }
    }

    public void incrementSourceImported(String sourceName, int importedCount) {
        if (importedCount > 0) {
            meterRegistry.counter("proxy.imported.by_source.total", "source", normalizeSourceTag(sourceName))
                    .increment(importedCount);
        }
    }

    public void incrementSourceSkipped(String sourceName, int skippedCount) {
        if (skippedCount > 0) {
            meterRegistry.counter("proxy.import.skipped.by_source.total", "source", normalizeSourceTag(sourceName))
                    .increment(skippedCount);
        }
    }

    public void incrementSourceRejected(String sourceName, int rejectedCount) {
        if (rejectedCount > 0) {
            meterRegistry.counter("proxy.import.rejected.by_source.total", "source", normalizeSourceTag(sourceName))
                    .increment(rejectedCount);
        }
    }

    public void incrementSourceRejectedByReason(String sourceName, RawProxyRejectReason rejectReason, int rejectedCount) {
        if (rejectedCount > 0) {
            meterRegistry.counter(
                    "proxy.import.rejected.by_source_reason.total",
                    "source", normalizeSourceTag(sourceName),
                    "reason", normalizeRejectReasonTag(rejectReason)
            ).increment(rejectedCount);
        }
    }

    public void incrementSourceFailure(String sourceName) {
        meterRegistry.counter("proxy.import.failure.by_source.total", "source", normalizeSourceTag(sourceName))
                .increment();
    }

    public void incrementCheckSuccess() {
        checkSuccessCounter.increment();
    }

    public void incrementCheckFailure() {
        checkFailureCounter.increment();
    }

    public void incrementDeepProbeSuccess() {
        meterRegistry.counter("proxy.deep_probe.total", "outcome", "success").increment();
    }

    public void incrementDeepProbeFailure(MtProtoProbeFailureCode failureCode) {
        String tagValue = failureCode != null ? failureCode.name() : "UNKNOWN";
        meterRegistry.counter("proxy.deep_probe.total", "outcome", "failure", "failure_code", tagValue).increment();
    }

    public void incrementFeedbackSubmitted(ProxyFeedbackPlatform platform, ProxyFeedbackResult result) {
        meterRegistry.counter(
                "proxy.feedback.submitted.total",
                "platform", normalizeFeedbackPlatformTag(platform),
                "result", normalizeFeedbackResultTag(result)
        ).increment();
    }

    public void recordImportDuration(String sourceName, Duration duration, boolean success) {
        if (duration == null || duration.isNegative()) {
            return;
        }

        Timer.builder("proxy.import.duration")
                .tag("source", normalizeSourceTag(sourceName))
                .tag("outcome", success ? "success" : "failure")
                .register(meterRegistry)
                .record(duration);
    }

    public void recordCheckCycleDuration(String trigger, String selection, Duration duration, boolean success) {
        if (duration == null || duration.isNegative()) {
            return;
        }

        Timer.builder("proxy.check.cycle.duration")
                .tag("trigger", normalizeTag(trigger))
                .tag("selection", normalizeTag(selection))
                .tag("outcome", success ? "success" : "failure")
                .register(meterRegistry)
                .record(duration);
    }

    public void incrementCheckCycleSkipped(String trigger) {
        meterRegistry.counter("proxy.check.cycle.skipped.total", "trigger", normalizeTag(trigger)).increment();
    }

    private String normalizeSourceTag(String sourceName) {
        return sourceName != null && !sourceName.isBlank() ? sourceName : "unknown";
    }

    private String normalizeTag(String value) {
        return value != null && !value.isBlank() ? value : "unknown";
    }

    private String normalizeRejectReasonTag(RawProxyRejectReason rejectReason) {
        return rejectReason != null ? rejectReason.name() : "UNKNOWN";
    }

    private String normalizeFeedbackPlatformTag(ProxyFeedbackPlatform platform) {
        return platform != null ? platform.name() : "UNKNOWN";
    }

    private String normalizeFeedbackResultTag(ProxyFeedbackResult result) {
        return result != null ? result.name() : "UNKNOWN";
    }
}
