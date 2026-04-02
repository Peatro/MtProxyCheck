package com.peatroxd.mtprototest.proxy.repository;

import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProxyFeedbackRepository extends JpaRepository<ProxyFeedbackEntity, Long> {
}
