package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;

public interface ProxyCheckExecutionService {
    ProxyCheckExecution execute(ProxyEntity proxy, boolean allowDeepProbe);
}
