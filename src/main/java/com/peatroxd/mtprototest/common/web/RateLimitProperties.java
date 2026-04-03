package com.peatroxd.mtprototest.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int publicReadLimit = 120;
    private long publicReadWindowMs = 60_000;
    private int feedbackWriteLimit = 20;
    private long feedbackWriteWindowMs = 60_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPublicReadLimit() {
        return publicReadLimit;
    }

    public void setPublicReadLimit(int publicReadLimit) {
        this.publicReadLimit = publicReadLimit;
    }

    public long getPublicReadWindowMs() {
        return publicReadWindowMs;
    }

    public void setPublicReadWindowMs(long publicReadWindowMs) {
        this.publicReadWindowMs = publicReadWindowMs;
    }

    public int getFeedbackWriteLimit() {
        return feedbackWriteLimit;
    }

    public void setFeedbackWriteLimit(int feedbackWriteLimit) {
        this.feedbackWriteLimit = feedbackWriteLimit;
    }

    public long getFeedbackWriteWindowMs() {
        return feedbackWriteWindowMs;
    }

    public void setFeedbackWriteWindowMs(long feedbackWriteWindowMs) {
        this.feedbackWriteWindowMs = feedbackWriteWindowMs;
    }
}
