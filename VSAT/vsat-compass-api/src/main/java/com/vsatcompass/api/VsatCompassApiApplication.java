package com.vsatcompass.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VsatCompassApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VsatCompassApiApplication.class, args);
    }
}
