package com.peatroxd.mtprototest.checker.repository;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProxyCheckHistoryRepository extends JpaRepository<ProxyCheckHistoryEntity, Long> {

    List<ProxyCheckHistoryEntity> findTop20ByProxyIdOrderByCheckedAtDesc(Long proxyId);

    long countByCheckedAtAfter(LocalDateTime checkedAfter);

    long countByCheckedAtAfterAndAliveIsTrue(LocalDateTime checkedAfter);

    long countByCheckedAtAfterAndAliveIsFalse(LocalDateTime checkedAfter);

    long countByCheckedAtAfterAndCheckTypeAndAliveIsTrue(LocalDateTime checkedAfter, ProxyCheckType checkType);

    long countByCheckedAtAfterAndCheckTypeAndAliveIsFalse(LocalDateTime checkedAfter, ProxyCheckType checkType);

    List<ProxyCheckHistoryEntity> findTop50ByCheckTypeAndFailureCodeIsNotNullOrderByCheckedAtDesc(ProxyCheckType checkType);

    long countByCheckTypeAndFailureCode(ProxyCheckType checkType, MtProtoProbeFailureCode failureCode);
}
