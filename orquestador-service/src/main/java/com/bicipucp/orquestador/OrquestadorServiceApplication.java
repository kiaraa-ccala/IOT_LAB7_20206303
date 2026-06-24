package com.bicipucp.orquestador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class OrquestadorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrquestadorServiceApplication.class, args);
    }
}
