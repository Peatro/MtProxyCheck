package com.peatroxd.mtprototest.proxy.repository;

import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
              and p.moderationStatus <> com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus.BLACKLISTED
            order by p.score desc, p.lastLatencyMs asc nulls last
            """)
    List<ProxyEntity> findTop200ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus status);

    @Query("""
            select p from ProxyEntity p
            where p.status <> com.peatroxd.mtprototest.proxy.enums.ProxyStatus.ARCHIVED
              and p.moderationStatus <> com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus.BLACKLISTED
            order by
              case
                when p.status = com.peatroxd.mtprototest.proxy.enums.ProxyStatus.NEW then 0
                when p.status = com.peatroxd.mtprototest.proxy.enums.ProxyStatus.ALIVE
                     and p.verificationStatus = com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus.QUICK_OK then 1
                when p.status = com.peatroxd.mtprototest.proxy.enums.ProxyStatus.ALIVE
                     and p.verificationStatus = com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus.VERIFIED then 2
                when p.status = com.peatroxd.mtprototest.proxy.enums.ProxyStatus.DEAD then 3
                else 4
              end,
              p.lastCheckedAt asc nulls first,
              p.score desc,
              p.id asc
            """)
    List<ProxyEntity> findStartupBatch(Pageable pageable);

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
              and p.moderationStatus <> com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus.BLACKLISTED
            order by p.createdAt asc, p.id asc
            """)
    List<ProxyEntity> findLifecycleBatchByStatus(ProxyStatus status, Pageable pageable);

    @Query("""
            select p from ProxyEntity p
            where p.status = :status
              and p.moderationStatus <> com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus.BLACKLISTED
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
              and p.moderationStatus <> com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus.BLACKLISTED
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

    long countByModerationStatus(ProxyModerationStatus moderationStatus);

    @Modifying
    @Transactional
    @Query("""
            update ProxyEntity p
               set p.status = :archivedStatus,
                   p.updatedAt = :updatedAt
             where p.status = :deadStatus
               and p.consecutiveFailures >= :minConsecutiveFailures
               and (
                    (p.lastSuccessAt is not null and p.lastSuccessAt <= :staleBefore)
                    or
                    (p.lastSuccessAt is null and p.createdAt <= :staleBefore)
               )
            """)
    int archiveDeadProxies(
            ProxyStatus deadStatus,
            ProxyStatus archivedStatus,
            int minConsecutiveFailures,
            LocalDateTime staleBefore,
            LocalDateTime updatedAt
    );
}
