package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.ProxyConnectivityChecker;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class SocketProxyConnectivityChecker implements ProxyConnectivityChecker {

    private static final int CONNECT_TIMEOUT_MS = 2500;

    @Override
    public ProxyCheckResult check(ProxyEntity proxy) {
        long startedAt = System.nanoTime();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), CONNECT_TIMEOUT_MS);

            long latency = (System.nanoTime() - startedAt) / 1_000_000;

            return new ProxyCheckResult(true, latency, ProxyVerificationStatus.QUICK_OK, null, null);

        } catch (Exception e) {
            return new ProxyCheckResult(false, -1, ProxyVerificationStatus.UNVERIFIED, null, e.getMessage());
        }
    }
}
