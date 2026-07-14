package com.peatroxd.mtprototest.checker.config;

import com.peatroxd.mtprototest.bootstrap.StartupProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties({CheckerExecutorProperties.class, CheckerProperties.class, TdlibProperties.class, StartupProperties.class})
public class CheckerExecutorConfig {

    @Bean
    public Executor proxyCheckerExecutor(CheckerExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    // Выделенный bounded-пул для E2E-пингов: изолирует TDLib-нагрузку от digest-батча.
    @Bean
    public Executor tdlibProbeExecutor(TdlibProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getConcurrency());
        executor.setMaxPoolSize(properties.getConcurrency());
        executor.setQueueCapacity(properties.getBatchLimit());
        executor.setThreadNamePrefix("tdlib-probe-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }
}
