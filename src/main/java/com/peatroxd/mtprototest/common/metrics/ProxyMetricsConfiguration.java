package com.peatroxd.mtprototest.common.metrics;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyMetricsConfiguration {

    @Bean
    public Object proxyGaugeRegistrations(MeterRegistry meterRegistry, ProxyRepository proxyRepository) {
        Gauge.builder("proxy.state.count", proxyRepository, repository -> repository.countByStatus(ProxyStatus.ALIVE))
                .tag("status", "ALIVE")
                .register(meterRegistry);
        Gauge.builder("proxy.state.count", proxyRepository, repository -> repository.countByStatus(ProxyStatus.DEAD))
                .tag("status", "DEAD")
                .register(meterRegistry);
        Gauge.builder("proxy.state.count", proxyRepository, repository -> repository.countByStatus(ProxyStatus.ARCHIVED))
                .tag("status", "ARCHIVED")
                .register(meterRegistry);
        Gauge.builder("proxy.state.count", proxyRepository, repository -> repository.countByStatus(ProxyStatus.NEW))
                .tag("status", "NEW")
                .register(meterRegistry);
        Gauge.builder("proxy.verification.count", proxyRepository, repository -> repository.countByVerificationStatus(ProxyVerificationStatus.VERIFIED))
                .tag("verification_status", "VERIFIED")
                .register(meterRegistry);
        Gauge.builder("proxy.verification.count", proxyRepository, repository -> repository.countByVerificationStatus(ProxyVerificationStatus.QUICK_OK))
                .tag("verification_status", "QUICK_OK")
                .register(meterRegistry);
        Gauge.builder("proxy.verification.count", proxyRepository, repository -> repository.countByVerificationStatus(ProxyVerificationStatus.UNVERIFIED))
                .tag("verification_status", "UNVERIFIED")
                .register(meterRegistry);
        return new Object();
    }
}
