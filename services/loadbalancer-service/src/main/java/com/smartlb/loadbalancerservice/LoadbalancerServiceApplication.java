package com.smartlb.loadbalancerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoadbalancerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoadbalancerServiceApplication.class, args);
    }
}