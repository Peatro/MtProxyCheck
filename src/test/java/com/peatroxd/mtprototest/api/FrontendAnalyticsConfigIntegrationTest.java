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
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mtprototest_frontend_config;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "app.analytics.posthog.enabled=true",
                "app.analytics.posthog.api-key=phc_test_key",
                "app.analytics.posthog.host=https://eu.i.posthog.com"
        }
)
@ActiveProfiles("test")
class FrontendAnalyticsConfigIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldExposePublicFrontendAnalyticsConfig() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/frontend-config"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"posthogEnabled\":true");
        assertThat(response.body()).contains("\"posthogApiKey\":\"phc_test_key\"");
        assertThat(response.body()).contains("\"posthogHost\":\"https://eu.i.posthog.com\"");
    }
}
