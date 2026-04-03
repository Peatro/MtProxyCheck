package com.peatroxd.mtprototest.admin.service;

import java.util.Collection;

public interface ProxyImportTrackingService {
    void markStarted(String source);
    void markFinished(String source, int imported, int skipped, int rejected);
    void markFailed(String source, String errorMessage);
    Collection<SourceImportSnapshot> getSnapshots();
}
