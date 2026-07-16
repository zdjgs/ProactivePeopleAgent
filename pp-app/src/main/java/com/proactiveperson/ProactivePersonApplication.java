package com.proactiveperson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.proactiveperson")
public class ProactivePersonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProactivePersonApplication.class, args);
    }
}
