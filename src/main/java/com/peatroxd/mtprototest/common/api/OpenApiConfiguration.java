package com.peatroxd.mtprototest.common.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI mtprotoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mtprototest API")
                .version("v1")
                .description("Public B2C API for discovering and ranking Telegram MTProto proxies.")
                .contact(new Contact().name("Mtprototest"))
                .license(new License().name("Proprietary")));
    }
}
