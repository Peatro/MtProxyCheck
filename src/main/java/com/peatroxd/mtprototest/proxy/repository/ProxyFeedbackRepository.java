package com.peatroxd.mtprototest.proxy.repository;

import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProxyFeedbackRepository extends JpaRepository<ProxyFeedbackEntity, Long> {

    boolean existsByProxyIdAndPlatformAndClientKeyAndWindowStartedAt(
            Long proxyId,
            com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform platform,
            String clientKey,
            LocalDateTime windowStartedAt
    );

    List<ProxyFeedbackEntity> findTop20ByProxyIdOrderByCreatedAtDesc(Long proxyId);
}
