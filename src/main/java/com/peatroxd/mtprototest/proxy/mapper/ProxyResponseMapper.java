package com.peatroxd.mtprototest.proxy.mapper;

import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ProxyResponseMapper {

    public ProxyResponse toResponse(ProxyEntity proxy) {
        String encodedHost = encode(proxy.getHost());
        String encodedPort = encode(String.valueOf(proxy.getPort()));
        String encodedSecret = encode(proxy.getSecret());

        String tgLink = "tg://proxy?server=" + encodedHost
                + "&port=" + encodedPort
                + "&secret=" + encodedSecret;

        String webLink = "https://t.me/proxy?server=" + encodedHost
                + "&port=" + encodedPort
                + "&secret=" + encodedSecret;

        return ProxyResponse.builder()
                .id(proxy.getId())
                .host(proxy.getHost())
                .port(proxy.getPort())
                .secret(proxy.getSecret())
                .type(proxy.getType().name())
                .source(proxy.getSource())
                .status(proxy.getStatus().name())
                .verificationStatus(proxy.getVerificationStatus().name())
                .verified(proxy.getVerificationStatus() == com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus.VERIFIED)
                .score(proxy.getScore())
                .lastLatencyMs(proxy.getLastLatencyMs())
                .lastCheckedAt(proxy.getLastCheckedAt())
                .lastSuccessAt(proxy.getLastSuccessAt())
                .consecutiveFailures(proxy.getConsecutiveFailures())
                .consecutiveSuccesses(proxy.getConsecutiveSuccesses())
                .telegramDeepLink(tgLink)
                .telegramWebLink(webLink)
                .build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
