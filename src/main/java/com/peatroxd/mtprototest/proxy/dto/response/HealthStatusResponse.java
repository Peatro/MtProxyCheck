package com.peatroxd.mtprototest.proxy.dto.response;

import java.time.LocalDateTime;

public record HealthStatusResponse(
        boolean verificationHealthy,
        LocalDateTime lastCheckedAt,
        Long ageSeconds,
        String egressCountry
) {}
