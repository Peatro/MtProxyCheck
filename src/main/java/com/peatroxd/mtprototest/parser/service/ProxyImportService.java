package com.peatroxd.mtprototest.parser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.parser.config.ParserSourcesProperties;
import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.parser.source.ProxySource;
import com.peatroxd.mtprototest.parser.source.HttpProxySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ParserSourcesProperties.class)
public class ProxyImportService {

    private final ParserSourcesProperties parserSourcesProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final RawProxyNormalizer rawProxyNormalizer;
    private final ProxyRepository proxyRepository;
    private final ProxyMetricsService proxyMetricsService;

    public void importAll() {
        List<ProxySource> proxySources = configuredSources();
        if (proxySources.isEmpty()) {
            log.warn("No proxy sources are configured");
            return;
        }

        for (ProxySource source : proxySources) {
            try {
                importFromSource(source);
            } catch (Exception e) {
                log.error("Import failed for source='{}': {}", source.sourceName(), e.getMessage(), e);
                proxyMetricsService.incrementSourceFailure(source.sourceName());
            }
        }
    }

    public void importFromSource(ProxySource source) {
        List<RawProxy> rawProxies = source.fetch();

        int imported = 0;
        int skipped = 0;
        int rejected = 0;

        for (RawProxy rawProxy : rawProxies) {
            RawProxy normalized = rawProxyNormalizer.normalize(rawProxy).orElse(null);
            if (normalized == null) {
                rejected++;
                continue;
            }

            if (exists(normalized)) {
                skipped++;
                continue;
            }

            proxyRepository.save(toNewEntity(normalized));
            imported++;
        }

        log.info(
                "Import finished for source='{}': total={}, imported={}, skipped={}, rejected={}",
                source.sourceName(),
                rawProxies.size(),
                imported,
                skipped,
                rejected
        );
        proxyMetricsService.incrementImported(imported);
        proxyMetricsService.incrementSourceImported(source.sourceName(), imported);
        proxyMetricsService.incrementSourceSkipped(source.sourceName(), skipped);
        proxyMetricsService.incrementSourceRejected(source.sourceName(), rejected);
    }

    private List<ProxySource> configuredSources() {
        return parserSourcesProperties.getEntries().stream()
                .filter(ParserSourcesProperties.SourceDefinition::isEnabled)
                .filter(definition -> definition.getName() != null && !definition.getName().isBlank())
                .filter(definition -> definition.getUrl() != null && !definition.getUrl().isBlank())
                .map(definition -> new HttpProxySource(restClientBuilder, objectMapper, definition))
                .map(ProxySource.class::cast)
                .toList();
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
                .consecutiveFailures(0)
                .consecutiveSuccesses(0)
                .build();
    }
}
