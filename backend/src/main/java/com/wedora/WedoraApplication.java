package com.wedora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WedoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(WedoraApplication.class, args);
    }
}
