package com.peatroxd.mtprototest.proxy.service;

import com.peatroxd.mtprototest.proxy.dto.request.ProxyListRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyPageResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyStatsResponse;

import java.util.List;

public interface ProxyService {
    List<ProxyResponse> getBest();

    ProxyPageResponse getProxies(ProxyListRequest request);

    ProxyResponse getById(Long proxyId);

    ProxyStatsResponse getStats();
}
