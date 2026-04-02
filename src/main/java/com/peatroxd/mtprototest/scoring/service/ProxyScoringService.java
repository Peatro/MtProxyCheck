package com.peatroxd.mtprototest.scoring.service;

import com.peatroxd.mtprototest.scoring.model.ProxyScoreContext;

public interface ProxyScoringService {
    int calculateScore(ProxyScoreContext context);
}
