package com.peatroxd.mtprototest.api;

import org.junit.jupiter.api.Test;
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
        properties = "spring.datasource.url=jdbc:h2:mem:mtprototest_admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
)
@ActiveProfiles("test")
class AdminEndpointsSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldRejectManualImportWithoutAdminKey() throws Exception {
        HttpResponse<String> response = post("/api/v1/import/proxies", null);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"FORBIDDEN\"");
        assertThat(response.body()).contains("\"message\":\"Admin access required\"");
    }

    @Test
    void shouldAllowManualImportWithAdminKey() throws Exception {
        HttpResponse<String> response = post("/api/v1/import/proxies", "test-admin-key");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Proxy import started and completed");
    }

    @Test
    void shouldRejectManualCheckWithoutAdminKey() throws Exception {
        HttpResponse<String> response = post("/api/v1/check/proxies", null);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"FORBIDDEN\"");
        assertThat(response.body()).contains("\"message\":\"Admin access required\"");
    }

    private HttpResponse<String> post(String path, String adminKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        if (adminKey != null) {
            builder.header("X-Admin-Key", adminKey);
        }

        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
