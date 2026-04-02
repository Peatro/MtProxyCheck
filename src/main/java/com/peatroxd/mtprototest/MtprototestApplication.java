package com.peatroxd.mtprototest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MtprototestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtprototestApplication.class, args);
    }

}
