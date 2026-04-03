package com.peatroxd.mtprototest.bootstrap;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.parser.service.ProxyImportService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProxyStartupBootstrapTest {

    @Test
    void shouldUseDedicatedStartupBudgetInsteadOfFullCatalogCheck() throws Exception {
        ProxyImportService proxyImportService = mock(ProxyImportService.class);
        ProxyBatchCheckService proxyBatchCheckService = mock(ProxyBatchCheckService.class);
        ProxyCheckRunCoordinator coordinator = new ProxyCheckRunCoordinator();
        StartupProperties startupProperties = new StartupProperties();
        startupProperties.setCheckBatchSize(60);
        startupProperties.setDeepProbeLimit(8);

        ProxyStartupBootstrap bootstrap = new ProxyStartupBootstrap(
                proxyImportService,
                proxyBatchCheckService,
                coordinator,
                startupProperties
        );

        when(proxyBatchCheckService.checkStartupProxies(60, 8))
                .thenReturn(new ProxyBatchCheckSummary(10, 4, 4, 2));

        bootstrap.run(null);

        verify(proxyImportService).importAll();
        verify(proxyBatchCheckService).checkStartupProxies(60, 8);
        verify(proxyBatchCheckService, never()).checkAllProxies();
    }
}
