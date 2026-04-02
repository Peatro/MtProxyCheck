package com.peatroxd.mtprototest.parser.scheduler;

import com.peatroxd.mtprototest.parser.service.ProxyImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyImportScheduler {

    private final ProxyImportService proxyImportService;

    @Scheduled(
            initialDelayString = "${app.parser.initial-delay-ms:43200000}",
            fixedDelayString = "${app.parser.fixed-delay-ms:43200000}"
    )
    public void importProxies() {
        log.info("Starting scheduled proxy import");
        proxyImportService.importAll();
        log.info("Scheduled proxy import finished");
    }
}

