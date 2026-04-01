package com.peatroxd.mtprototest.proxy.repository;

import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyRepository extends JpaRepository<ProxyEntity, Long> {

    Optional<ProxyEntity> findByHostAndPortAndSecretAndType(
            String host,
            Integer port,
            String secret,
            ProxyType type
    );

    List<ProxyEntity> findTop200ByStatusOrderByIdAsc(ProxyStatus status);

    List<ProxyEntity> findTop50ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus status);
}
