package com.peatroxd.mtprototest.common.api;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details
) {
}
