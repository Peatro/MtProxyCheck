package com.peatroxd.mtprototest.admin.service.impl;

import com.peatroxd.mtprototest.admin.service.ProxyImportTrackingService;
import com.peatroxd.mtprototest.admin.service.SourceImportSnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryProxyImportTrackingService implements ProxyImportTrackingService {

    private final Map<String, SourceImportSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void markStarted(String source) {
        snapshots.compute(source, (ignored, existing) -> new SourceImportSnapshot(
                source,
                LocalDateTime.now(),
                existing != null ? existing.completedAt() : null,
                existing != null && existing.succeeded(),
                existing != null ? existing.imported() : 0,
                existing != null ? existing.skipped() : 0,
                existing != null ? existing.rejected() : 0,
                existing != null ? existing.errorMessage() : null
        ));
    }

    @Override
    public void markFinished(String source, int imported, int skipped, int rejected) {
        snapshots.compute(source, (ignored, existing) -> new SourceImportSnapshot(
                source,
                existing != null ? existing.startedAt() : LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                imported,
                skipped,
                rejected,
                null
        ));
    }

    @Override
    public void markFailed(String source, String errorMessage) {
        snapshots.compute(source, (ignored, existing) -> new SourceImportSnapshot(
                source,
                existing != null ? existing.startedAt() : LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                existing != null ? existing.imported() : 0,
                existing != null ? existing.skipped() : 0,
                existing != null ? existing.rejected() : 0,
                errorMessage
        ));
    }

    @Override
    public Collection<SourceImportSnapshot> getSnapshots() {
        return snapshots.values().stream()
                .sorted(Comparator.comparing(SourceImportSnapshot::source))
                .toList();
    }
}
