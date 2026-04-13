package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:mtprototest_prometheus;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
)
@ActiveProfiles("test")
class PrometheusMetricsExposureIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ProxyMetricsService proxyMetricsService;

    @Test
    void shouldExposeSystemHttpAndCustomProxyMetrics() throws Exception {
        proxyRepository.saveAllAndFlush(List.of(
                proxy("40.40.40.40", ProxyStatus.ALIVE, ProxyVerificationStatus.VERIFIED),
                proxy("41.41.41.41", ProxyStatus.DEAD, ProxyVerificationStatus.QUICK_OK),
                proxy("42.42.42.42", ProxyStatus.NEW, ProxyVerificationStatus.UNVERIFIED)
        ));

        proxyMetricsService.incrementImported(2);
        proxyMetricsService.incrementSourceImported("manual", 2);
        proxyMetricsService.incrementSourceRejected("manual", 1);
        proxyMetricsService.incrementFeedbackSubmitted(ProxyFeedbackPlatform.DESKTOP, ProxyFeedbackResult.WORKED);
        proxyMetricsService.recordImportDuration("manual", Duration.ofMillis(250), true);
        proxyMetricsService.recordCheckCycleDuration("scheduled", "alive", Duration.ofMillis(500), true);
        proxyMetricsService.incrementCheckCycleSkipped("scheduled");
        proxyMetricsService.incrementDeepProbeSuccess();

        send("/api/v1/proxies/stats");
        HttpResponse<String> prometheus = send("/actuator/prometheus");

        assertThat(prometheus.statusCode()).isEqualTo(200);
        assertThat(prometheus.body()).contains("jvm_memory_used_bytes");
        assertThat(prometheus.body()).contains("process_uptime_seconds");
        assertThat(prometheus.body()).contains("http_server_requests_seconds_count");
        assertThat(prometheus.body()).contains("proxy_state_count");
        assertThat(prometheus.body()).contains("proxy_verification_count");
        assertThat(prometheus.body()).contains("proxy_imported_total");
        assertThat(prometheus.body()).contains("proxy_feedback_submitted_total");
        assertThat(prometheus.body()).contains("proxy_import_duration_seconds_count");
        assertThat(prometheus.body()).contains("proxy_check_cycle_duration_seconds_count");
        assertThat(prometheus.body()).contains("proxy_check_cycle_skipped_total");
        assertThat(prometheus.body()).contains("proxy_deep_probe_total");
    }

    private HttpResponse<String> send(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/plain,application/json")
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private ProxyEntity proxy(String host, ProxyStatus status, ProxyVerificationStatus verificationStatus) {
        return ProxyEntity.builder()
                .host(host)
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("prometheus_test")
                .status(status)
                .verificationStatus(verificationStatus)
                .score(50)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build();
    }
}
