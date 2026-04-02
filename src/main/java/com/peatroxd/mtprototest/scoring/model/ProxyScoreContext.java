package com.peatroxd.mtprototest.scoring.model;

import com.peatroxd.mtprototest.checker.entity.ProxyCheckHistoryEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.entity.ProxyFeedbackEntity;

import java.util.List;

public record ProxyScoreContext(
        ProxyEntity proxy,
        List<ProxyCheckHistoryEntity> recentChecks,
        List<ProxyFeedbackEntity> recentFeedback
) {
}
