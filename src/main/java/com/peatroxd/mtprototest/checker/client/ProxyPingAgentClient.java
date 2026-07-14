package com.peatroxd.mtprototest.checker.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.peatroxd.mtprototest.checker.config.TdlibProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент к внешнему TDLib-прокси-агенту (end-to-end проверка MTProto).
 * Любая ошибка транспорта/парсинга схлопывается в PingResult{alive:false} — наружу не кидаем.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyPingAgentClient {

    private final RestClient.Builder restClientBuilder;
    private final TdlibProperties properties;

    private RestClient client;
    private RestClient healthClient;

    @PostConstruct
    void init() {
        this.client = buildClient(properties.getRequestTimeoutMs());
        // health-guard — короткий таймаут, чтобы недоступный агент не подвешивал цикл
        this.healthClient = buildClient(properties.getHealthTimeoutMs());
    }

    private RestClient buildClient(int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        // clone(): restClientBuilder — общий singleton (использует и парсер),
        // baseUrl/requestFactory ставим на изолированную копию, не мутируя бин
        return restClientBuilder.clone()
                .baseUrl(properties.getAgentUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public PingResult probe(String host, int port, String secretHex) {
        try {
            PingResult result = client.post()
                    .uri("/ping")
                    .body(new PingRequest(host, port, secretHex))
                    .retrieve()
                    .body(PingResult.class);
            return result != null ? result : new PingResult(false, null, "empty response");
        } catch (Exception e) {
            log.debug("E2E ping failed for {}:{}: {}", host, port, e.getMessage());
            return new PingResult(false, null, e.getMessage());
        }
    }

    public boolean healthy() {
        try {
            HealthResponse health = healthClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(HealthResponse.class);
            return health != null && health.ok();
        } catch (Exception e) {
            log.debug("TDLib agent health check failed: {}", e.getMessage());
            return false;
        }
    }

    record PingRequest(String host, int port, String secret) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PingResult(boolean alive, Double seconds, String error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HealthResponse(boolean ok) {
    }
}
