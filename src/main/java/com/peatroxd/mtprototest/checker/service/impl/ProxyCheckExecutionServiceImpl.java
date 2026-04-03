package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.checker.model.MtProtoDeepProbeResult;
import com.peatroxd.mtprototest.checker.model.ProxyCheckExecution;
import com.peatroxd.mtprototest.checker.model.ProxyCheckHistoryRecord;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.MtProtoDeepProbeService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckExecutionService;
import com.peatroxd.mtprototest.checker.service.ProxyConnectivityChecker;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProxyCheckExecutionServiceImpl implements ProxyCheckExecutionService {

    private final ProxyConnectivityChecker proxyConnectivityChecker;
    private final MtProtoDeepProbeService mtProtoDeepProbeService;
    private final ProxyMetricsService proxyMetricsService;

    public ProxyCheckExecutionServiceImpl(
            ProxyConnectivityChecker proxyConnectivityChecker,
            MtProtoDeepProbeService mtProtoDeepProbeService,
            ProxyMetricsService proxyMetricsService
    ) {
        this.proxyConnectivityChecker = proxyConnectivityChecker;
        this.mtProtoDeepProbeService = mtProtoDeepProbeService;
        this.proxyMetricsService = proxyMetricsService;
    }

    @Override
    public ProxyCheckExecution execute(ProxyEntity proxy, boolean allowDeepProbe) {
        List<ProxyCheckHistoryRecord> historyRecords = new ArrayList<>();

        ProxyCheckResult quickResult = proxyConnectivityChecker.check(proxy);
        historyRecords.add(historyRecord(LocalDateTime.now(), ProxyCheckType.QUICK, quickResult));

        if (!quickResult.alive() || !allowDeepProbe) {
            return new ProxyCheckExecution(quickResult, historyRecords);
        }

        MtProtoDeepProbeResult deepResult = mtProtoDeepProbeService.probe(proxy);
        if (!deepResult.success()) {
            proxyMetricsService.incrementDeepProbeFailure(deepResult.failureCode());
            ProxyCheckResult quickOkResult = new ProxyCheckResult(
                    true,
                    quickResult.latencyMs(),
                    ProxyVerificationStatus.QUICK_OK,
                    deepResult.failureCode(),
                    deepResult.reason()
            );
            historyRecords.add(historyRecord(
                    LocalDateTime.now(),
                    ProxyCheckType.DEEP,
                    false,
                    ProxyVerificationStatus.UNVERIFIED,
                    null,
                    deepResult.failureCode(),
                    deepResult.reason()
            ));
            return new ProxyCheckExecution(quickOkResult, historyRecords);
        }

        proxyMetricsService.incrementDeepProbeSuccess();
        long latencyMs = deepResult.latencyMs() >= 0 ? deepResult.latencyMs() : quickResult.latencyMs();
        ProxyCheckResult verifiedResult = new ProxyCheckResult(
                true,
                latencyMs,
                ProxyVerificationStatus.VERIFIED,
                null,
                null
        );
        historyRecords.add(historyRecord(LocalDateTime.now(), ProxyCheckType.DEEP, verifiedResult));
        return new ProxyCheckExecution(verifiedResult, historyRecords);
    }

    private ProxyCheckHistoryRecord historyRecord(LocalDateTime checkedAt, ProxyCheckType checkType, ProxyCheckResult result) {
        return historyRecord(
                checkedAt,
                checkType,
                result.alive(),
                result.verificationStatus(),
                result.latencyMs() >= 0 ? result.latencyMs() : null,
                result.failureCode(),
                result.errorMessage()
        );
    }

    private ProxyCheckHistoryRecord historyRecord(
            LocalDateTime checkedAt,
            ProxyCheckType checkType,
            boolean alive,
            ProxyVerificationStatus verificationStatus,
            Long latencyMs,
            com.peatroxd.mtprototest.checker.model.MtProtoProbeFailureCode failureCode,
            String failureReason
    ) {
        return new ProxyCheckHistoryRecord(
                checkedAt,
                checkType,
                alive,
                verificationStatus,
                latencyMs,
                failureCode,
                failureReason
        );
    }
}
