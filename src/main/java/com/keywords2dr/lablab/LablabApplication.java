package com.keywords2dr.lablab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LablabApplication {

    public static void main(String[] args) {
        SpringApplication.run(LablabApplication.class, args);
    }
}