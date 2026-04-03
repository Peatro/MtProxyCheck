package com.peatroxd.mtprototest.admin.dto;

import com.peatroxd.mtprototest.proxy.enums.ProxyModerationStatus;
import jakarta.validation.constraints.NotNull;

public record AdminProxyModerationRequest(
        @NotNull(message = "Moderation status is required")
        ProxyModerationStatus moderationStatus
) {
}
