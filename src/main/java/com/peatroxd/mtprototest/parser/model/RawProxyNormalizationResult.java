package com.peatroxd.mtprototest.parser.model;

public record RawProxyNormalizationResult(
        RawProxy proxy,
        RawProxyRejectReason rejectReason
) {

    public static RawProxyNormalizationResult accepted(RawProxy proxy) {
        return new RawProxyNormalizationResult(proxy, null);
    }

    public static RawProxyNormalizationResult rejected(RawProxyRejectReason rejectReason) {
        return new RawProxyNormalizationResult(null, rejectReason);
    }

    public boolean accepted() {
        return proxy != null;
    }
}
