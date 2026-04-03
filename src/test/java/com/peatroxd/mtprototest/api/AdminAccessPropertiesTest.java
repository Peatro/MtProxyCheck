package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.web.AdminAccessProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessPropertiesTest {

    @Test
    void shouldFailFastWhenAdminProtectionEnabledWithoutKey() {
        AdminAccessProperties properties = new AdminAccessProperties();
        properties.setEnabled(true);
        properties.setHeaderName("X-Admin-Key");
        properties.setKey(" ");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.admin.key must be set");
    }

    @Test
    void shouldAllowEnabledAdminProtectionWithHeaderAndKey() {
        AdminAccessProperties properties = new AdminAccessProperties();
        properties.setEnabled(true);
        properties.setHeaderName("X-Admin-Key");
        properties.setKey("test-admin-key");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}
