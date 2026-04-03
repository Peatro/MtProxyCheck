package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.service.ProxyRetentionService;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyRetentionServiceImpl implements ProxyRetentionService {

    private final ProxyRepository proxyRepository;
    private final CheckerProperties checkerProperties;

    @Override
    @Transactional
    public int archiveStaleDeadProxies() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusNanos(checkerProperties.getArchiveDeadAfterMs() * 1_000_000);

        int archived = proxyRepository.archiveDeadProxies(
                ProxyStatus.DEAD,
                ProxyStatus.ARCHIVED,
                checkerProperties.getArchiveMinConsecutiveFailures(),
                staleBefore,
                now
        );

        if (archived > 0) {
            log.info(
                    "Archived stale dead proxies: archived={}, staleBefore={}, minConsecutiveFailures={}",
                    archived,
                    staleBefore,
                    checkerProperties.getArchiveMinConsecutiveFailures()
            );
        }

        return archived;
    }
}
