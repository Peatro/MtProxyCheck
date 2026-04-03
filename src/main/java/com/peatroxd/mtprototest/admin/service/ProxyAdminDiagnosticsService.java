package com.peatroxd.mtprototest.admin.service;

import com.peatroxd.mtprototest.admin.dto.AdminDeepProbeFailuresResponse;
import com.peatroxd.mtprototest.admin.dto.AdminManualRecheckResponse;

public interface ProxyAdminDiagnosticsService {
    AdminManualRecheckResponse recheckProxy(Long proxyId);
    AdminDeepProbeFailuresResponse getDeepProbeFailures();
}
