package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
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
        properties = "spring.datasource.url=jdbc:h2:mem:mtprototest_openapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
)
@ActiveProfiles("test")
class OpenApiVersioningIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProxyRepository proxyRepository;

    @Test
    void shouldExposeOpenApiJsonAndSwaggerUi() throws Exception {
        HttpResponse<String> apiDocs = send("/v3/api-docs");
        HttpResponse<String> docsUi = send("/docs");

        assertThat(apiDocs.statusCode()).isEqualTo(200);
        assertThat(apiDocs.body()).contains("\"openapi\"");
        assertThat(apiDocs.body()).contains("\"Mtprototest API\"");
        assertThat(apiDocs.body()).contains("/api/v1/proxies");

        assertThat(docsUi.statusCode()).isIn(200, 302);
    }

    @Test
    void shouldServeVersionedBestProxyEndpoint() throws Exception {
        proxyRepository.saveAndFlush(ProxyEntity.builder()
                .host("20.20.20.20")
                .port(443)
                .secret("00112233445566778899aabbccddeeff")
                .type(ProxyType.MTPROTO)
                .source("test")
                .status(ProxyStatus.ALIVE)
                .verificationStatus(ProxyVerificationStatus.VERIFIED)
                .score(95)
                .consecutiveFailures(0)
                .consecutiveSuccesses(1)
                .build());

        HttpResponse<String> response = send("/api/v1/proxies/best");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"host\":\"20.20.20.20\"");
    }

    private HttpResponse<String> send(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json,text/html")
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
