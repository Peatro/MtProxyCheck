package com.peatroxd.mtprototest.proxy.dto.response;

import java.util.List;

public record ProxyPageResponse(
        List<ProxyResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
