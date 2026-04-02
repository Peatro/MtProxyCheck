package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;

public interface ProxyCheckUpdateService {
    void applyExecution(Long proxyId, ProxyCheckExecution execution);
}
