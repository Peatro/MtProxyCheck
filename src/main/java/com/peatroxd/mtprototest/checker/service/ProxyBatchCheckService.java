package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;

public interface ProxyBatchCheckService {
    ProxyBatchCheckSummary checkAllProxies();

    ProxyBatchCheckSummary checkNewProxies();

    ProxyBatchCheckSummary checkAliveQuickOkProxies();

    ProxyBatchCheckSummary checkAliveVerifiedProxies();

    ProxyBatchCheckSummary checkDeadProxies();
}
