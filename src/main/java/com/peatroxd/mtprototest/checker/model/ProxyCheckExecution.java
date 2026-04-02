package com.peatroxd.mtprototest.checker.model;

import java.util.List;

public record ProxyCheckExecution(
        ProxyCheckResult finalResult,
        List<ProxyCheckHistoryRecord> historyRecords
) {
}
