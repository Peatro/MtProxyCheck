package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

@Component
@RequiredArgsConstructor
public class ProbeSocketFactory {

    private final CheckerProperties checkerProperties;

    /** Прямой коннект из Майами, либо через SPb-туннель, если socks.enabled=true. */
    public Socket create() {
        var socks = checkerProperties.getSocks();
        if (socks.isEnabled()) {
            return new Socket(new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(socks.getHost(), socks.getPort())));
        }
        return new Socket();
    }
}