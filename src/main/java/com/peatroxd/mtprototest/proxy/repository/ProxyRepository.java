package com.peatroxd.mtprototest.proxy.repository;

import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyRepository extends JpaRepository<ProxyEntity, Long>, JpaSpecificationExecutor<ProxyEntity> {

    Optional<ProxyEntity> findByHostAndPortAndSecretAndType(
            String host,
            Integer port,
            String secret,
            ProxyType type
    );

    List<ProxyEntity> findAllByOrderByIdAsc();

    List<ProxyEntity> findTop200ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus status);

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
            order by p.createdAt asc, p.id asc
            """)
    List<ProxyEntity> findLifecycleBatchByStatus(ProxyStatus status, Pageable pageable);

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
              and p.verificationStatus = :verificationStatus
              and (p.lastCheckedAt is null or p.lastCheckedAt <= :checkedBefore)
            order by p.lastCheckedAt asc nulls first, p.id asc
            """)
    List<ProxyEntity> findLifecycleBatchByStatusAndVerificationStatus(
            ProxyStatus status,
            ProxyVerificationStatus verificationStatus,
            LocalDateTime checkedBefore,
            Pageable pageable
    );

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
              and (p.lastCheckedAt is null or p.lastCheckedAt <= :checkedBefore)
            order by p.lastCheckedAt asc nulls first, p.id asc
            """)
    List<ProxyEntity> findLifecycleBatchByStatusBefore(
            ProxyStatus status,
            LocalDateTime checkedBefore,
            Pageable pageable
    );

    long countByStatus(ProxyStatus status);

    long countByVerificationStatus(ProxyVerificationStatus verificationStatus);
}
