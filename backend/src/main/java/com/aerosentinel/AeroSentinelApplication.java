package com.aerosentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AeroSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(AeroSentinelApplication.class, args);
    }
}
