package com.peatroxd.mtprototest.common.metrics;

import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.parser.model.RawProxyRejectReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

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

    private String normalizeSourceTag(String sourceName) {
        return sourceName != null && !sourceName.isBlank() ? sourceName : "unknown";
    }

    private String normalizeRejectReasonTag(RawProxyRejectReason rejectReason) {
        return rejectReason != null ? rejectReason.name() : "UNKNOWN";
    }
}
