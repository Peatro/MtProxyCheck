package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import com.peatroxd.mtprototest.checker.repository.ProxyCheckHistoryRepository;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:mtprototest_admin_overview;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
)
@ActiveProfiles("test")
class AdminOverviewIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private ProxyCheckHistoryRepository proxyCheckHistoryRepository;

    @Test
    void shouldExposeProtectedAdminOverview() throws Exception {
        proxyRepository.saveAndFlush(ProxyEntity.builder()
                .host("30.30.30.30")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("source_alpha")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.VERIFIED)
                .moderationStatus(ProxyModerationStatus.WHITELISTED)
                .score(90)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build());

        proxyRepository.saveAndFlush(ProxyEntity.builder()
                .host("31.31.31.31")
                .port(443)
                .secret("00112233445566778899aabbccddeefe")
                .type(ProxyType.MTPROTO)
                .source("source_beta")
                .status(ProxyStatus.DEAD)
                .verificationStatus(ProxyVerificationStatus.UNVERIFIED)
                .moderationStatus(ProxyModerationStatus.BLACKLISTED)
                .score(10)
                .consecutiveFailures(2)
                .consecutiveSuccesses(0)
                .build());

        HttpResponse<String> unauthorized = get("/api/v1/admin/overview", null);
        HttpResponse<String> authorized = get("/api/v1/admin/overview", "test-admin-key");

        assertThat(unauthorized.statusCode()).isEqualTo(403);
        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(authorized.body()).contains("\"whitelistedCount\":1");
        assertThat(authorized.body()).contains("\"blacklistedCount\":1");
        assertThat(authorized.body()).contains("\"source\":\"source_alpha\"");
        assertThat(authorized.body()).contains("\"source\":\"source_beta\"");
    }

    @Test
    void shouldAllowUpdatingModerationStatus() throws Exception {
        ProxyEntity proxy = proxyRepository.saveAndFlush(ProxyEntity.builder()
                .host("32.32.32.32")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("source_gamma")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.VERIFIED)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(70)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build());

        HttpResponse<String> response = patch(
                "/api/v1/admin/proxies/" + proxy.getId() + "/moderation",
                "{\"moderationStatus\":\"BLACKLISTED\"}",
                "test-admin-key"
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"moderationStatus\":\"BLACKLISTED\"");
        assertThat(proxyRepository.findById(proxy.getId()).orElseThrow().getModerationStatus())
                .isEqualTo(ProxyModerationStatus.BLACKLISTED);
    }

    @Test
    void shouldExposeDeepProbeFailureDiagnostics() throws Exception {
        ProxyEntity proxy = proxyRepository.saveAndFlush(ProxyEntity.builder()
                .host("33.33.33.33")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("source_delta")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.QUICK_OK)
                .moderationStatus(ProxyModerationStatus.NORMAL)
                .score(55)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build());

        proxyCheckHistoryRepository.saveAndFlush(ProxyCheckHistoryEntity.builder()
                .proxy(proxy)
                .checkType(ProxyCheckType.DEEP)
                .alive(false)
                .verificationStatus(ProxyVerificationStatus.UNVERIFIED)
                .failureCode(MtProtoProbeFailureCode.CONNECT_ERROR)
                .failureReason("Connection timed out")
                .build());

        HttpResponse<String> response = get("/api/v1/admin/failures/deep-probe", "test-admin-key");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"failureCode\":\"CONNECT_ERROR\"");
        assertThat(response.body()).contains("\"host\":\"33.33.33.33\"");
        assertThat(response.body()).contains("\"source\":\"source_delta\"");
    }

    private HttpResponse<String> get(String path, String adminKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .GET();
        if (adminKey != null) {
            builder.header("X-Admin-Key", adminKey);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String body, String adminKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body));
        if (adminKey != null) {
            builder.header("X-Admin-Key", adminKey);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
