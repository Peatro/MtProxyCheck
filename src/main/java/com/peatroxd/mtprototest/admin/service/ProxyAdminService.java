package com.peatroxd.mtprototest.admin.service;

import com.peatroxd.mtprototest.admin.dto.AdminCatalogOverviewResponse;
import com.peatroxd.mtprototest.admin.dto.AdminProxyModerationResponse;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;

public interface ProxyAdminService {
    AdminCatalogOverviewResponse getOverview();
    AdminProxyModerationResponse updateModerationStatus(Long proxyId, ProxyModerationStatus moderationStatus);
}
