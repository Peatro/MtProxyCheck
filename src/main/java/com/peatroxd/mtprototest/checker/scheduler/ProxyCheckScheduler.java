package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.checker.service.ProxyRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyCheckScheduler {

    private final ProxyBatchCheckService proxyBatchCheckService;
    private final ProxyRetentionService proxyRetentionService;
    private final ProxyCheckRunCoordinator proxyCheckRunCoordinator;

    @Scheduled(
            initialDelayString = "${app.checker.initial-delay-ms:300000}",
            fixedDelayString = "${app.checker.fixed-delay-ms:300000}"
    )
    public void checkNewProxies() {
        if (!proxyCheckRunCoordinator.tryStartCatalogCycle("scheduled proxy check")) {
            return;
        }

        try {
            log.info("Starting scheduled proxy check");
            int archivedCount = proxyRetentionService.archiveStaleDeadProxies();
            ProxyBatchCheckSummary newSummary = proxyBatchCheckService.checkNewProxies();
            ProxyBatchCheckSummary aliveQuickOkSummary = proxyBatchCheckService.checkAliveQuickOkProxies();
            ProxyBatchCheckSummary aliveVerifiedSummary = proxyBatchCheckService.checkAliveVerifiedProxies();
            ProxyBatchCheckSummary deadSummary = proxyBatchCheckService.checkDeadProxies();
            log.info(
                    "Scheduled proxy check finished: archived={}, new(total={}, quickOk={}, verified={}, dead={}), aliveQuickOk(total={}, quickOk={}, verified={}, dead={}), aliveVerified(total={}, quickOk={}, verified={}, dead={}), dead(total={}, quickOk={}, verified={}, dead={})",
                    archivedCount,
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
            proxyCheckRunCoordinator.finishCatalogCycle();
        }
    }
}
