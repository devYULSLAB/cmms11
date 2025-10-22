package com.cmms11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Cmms11Application {
    public static void main(String[] args) {
        SpringApplication.run(Cmms11Application.class, args);
    }
}
