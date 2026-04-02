package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxySecretDetails;

public interface ProxySecretParser {
    ProxySecretDetails parse(String secretHex);
}
