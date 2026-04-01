package com.peatroxd.mtprototest.parser.service;

import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.parser.source.ProxySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyImportService {

    private final List<ProxySource> proxySources;
    private final RawProxyNormalizer rawProxyNormalizer;
    private final ProxyRepository proxyRepository;

    public void importAll() {
        for (ProxySource source : proxySources) {
            try {
                importFromSource(source);
            } catch (Exception e) {
                log.error("Import failed for source='{}': {}", source.sourceName(), e.getMessage(), e);
            }
        }
    }

    public void importFromSource(ProxySource source) {
        List<RawProxy> rawProxies = source.fetch();

        int imported = 0;
        int skipped = 0;

        for (RawProxy rawProxy : rawProxies) {
            RawProxy normalized = rawProxyNormalizer.normalize(rawProxy).orElse(null);
            if (normalized == null || exists(normalized)) {
                skipped++;
                continue;
            }

            proxyRepository.save(toNewEntity(normalized));
            imported++;
        }

        log.info(
                "Import finished for source='{}': total={}, imported={}, skipped={}",
                source.sourceName(),
                rawProxies.size(),
                imported,
                skipped
        );
    }

    private boolean exists(RawProxy proxy) {
        return proxyRepository.findByHostAndPortAndSecretAndType(
                proxy.host(),
                proxy.port(),
                proxy.secret(),
                proxy.type()
        ).isPresent();
    }

    private ProxyEntity toNewEntity(RawProxy proxy) {
        return ProxyEntity.builder()
                .host(proxy.host())
                .port(proxy.port())
                .secret(proxy.secret())
                .type(proxy.type())
                .source(proxy.source())
                .status(ProxyStatus.NEW)
                .score(0)
                .build();
    }
}
