package com.peatroxd.mtprototest.common.metrics;

import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
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
}
