package com.peatroxd.mtprototest.checker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ProxyCheckRunCoordinator {

    private final AtomicBoolean catalogCycleRunning = new AtomicBoolean(false);

    public boolean tryStartCatalogCycle(String triggerLabel) {
        if (catalogCycleRunning.compareAndSet(false, true)) {
            return true;
        }

        log.warn("Skipping {} because another proxy catalog cycle is already active", triggerLabel);
        return false;
    }

    public void finishCatalogCycle() {
        catalogCycleRunning.set(false);
    }
}
