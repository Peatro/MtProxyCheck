package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;

public interface ProxyCheckUpdateService {
    void applyExecution(Long proxyId, ProxyCheckExecution execution);

    /**
     * Применяет результат end-to-end проверки (TDLib-агент) к прокси.
     * Отдельная ось от connectivity: E2E-фейл не роняет в DEAD раньше 5 подряд.
     */
    void applyE2eResult(Long proxyId, boolean alive, Long latencyMs, String error);
}
