package com.peatroxd.mtprototest;

import com.peatroxd.mtprototest.bootstrap.StartupProperties;
import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mtprototest_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("smoke")
class SmokeProfileRuntimeIntegrationTest {

    @Autowired
    private StartupProperties startupProperties;

    @Autowired
    private CheckerProperties checkerProperties;

    @Value("${app.parser.initial-delay-ms}")
    private long parserInitialDelayMs;

    @Value("${app.parser.fixed-delay-ms}")
    private long parserFixedDelayMs;

    @Test
    void shouldApplySmokeRuntimeOverrides() {
        assertThat(startupProperties.isBootstrapEnabled()).isFalse();
        assertThat(parserInitialDelayMs).isEqualTo(3_600_000L);
        assertThat(parserFixedDelayMs).isEqualTo(3_600_000L);
        assertThat(checkerProperties.getInitialDelayMs()).isEqualTo(3_600_000L);
        assertThat(checkerProperties.getFixedDelayMs()).isEqualTo(3_600_000L);
    }
}
