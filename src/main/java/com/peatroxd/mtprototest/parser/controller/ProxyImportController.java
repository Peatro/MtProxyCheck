package com.peatroxd.mtprototest.parser.controller;

import com.peatroxd.mtprototest.parser.service.ProxyImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/import", "/api/v1/import"})
@RequiredArgsConstructor
public class ProxyImportController {

    private final ProxyImportService proxyImportService;

    @PostMapping("/proxies")
    public ResponseEntity<String> importProxies() {
        proxyImportService.importAll();
        return ResponseEntity.ok("Proxy import started and completed");
    }
}
