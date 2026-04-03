package com.peatroxd.mtprototest.common.web;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public class AdminAccessProperties {

    private boolean enabled = false;
    private String headerName = "X-Admin-Key";
    private String key;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @PostConstruct
    public void validate() {
        if (enabled && (key == null || key.isBlank())) {
            throw new IllegalStateException("app.admin.key must be set when admin access is enabled");
        }
        if (enabled && (headerName == null || headerName.isBlank())) {
            throw new IllegalStateException("app.admin.header-name must be set when admin access is enabled");
        }
    }

    public boolean isProtectionActive() {
        return enabled && key != null && !key.isBlank();
    }
}
