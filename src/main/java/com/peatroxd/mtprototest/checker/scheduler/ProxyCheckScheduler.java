package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyCheckScheduler {

    private final ProxyBatchCheckService proxyBatchCheckService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            initialDelayString = "${app.checker.initial-delay-ms:300000}",
            fixedDelayString = "${app.checker.fixed-delay-ms:300000}"
    )
    public void checkNewProxies() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping scheduled proxy check because previous run is still active");
            return;
        }

        try {
            log.info("Starting scheduled proxy check");
            ProxyBatchCheckSummary newSummary = proxyBatchCheckService.checkNewProxies();
            ProxyBatchCheckSummary aliveQuickOkSummary = proxyBatchCheckService.checkAliveQuickOkProxies();
            ProxyBatchCheckSummary aliveVerifiedSummary = proxyBatchCheckService.checkAliveVerifiedProxies();
            ProxyBatchCheckSummary deadSummary = proxyBatchCheckService.checkDeadProxies();
            log.info(
                    "Scheduled proxy check finished: new(total={}, quickOk={}, verified={}, dead={}), aliveQuickOk(total={}, quickOk={}, verified={}, dead={}), aliveVerified(total={}, quickOk={}, verified={}, dead={}), dead(total={}, quickOk={}, verified={}, dead={})",
                    newSummary.totalChecked(),
                    newSummary.quickOkCount(),
                    newSummary.verifiedCount(),
                    newSummary.deadCount(),
                    aliveQuickOkSummary.totalChecked(),
                    aliveQuickOkSummary.quickOkCount(),
                    aliveQuickOkSummary.verifiedCount(),
                    aliveQuickOkSummary.deadCount(),
                    aliveVerifiedSummary.totalChecked(),
                    aliveVerifiedSummary.quickOkCount(),
                    aliveVerifiedSummary.verifiedCount(),
                    aliveVerifiedSummary.deadCount(),
                    deadSummary.totalChecked(),
                    deadSummary.quickOkCount(),
                    deadSummary.verifiedCount(),
                    deadSummary.deadCount()
            );
        } finally {
            running.set(false);
        }
    }
}
