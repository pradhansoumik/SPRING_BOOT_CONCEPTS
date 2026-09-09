package com.example.autoconfigdemo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Scan starts here → only {@code com.example.autoconfigdemo.app} (+ subpackages).
 * {@code GreetAutoConfiguration} lives in {@code ...autoconfigure} so it is NOT scanned.
 * It loads only via {@code AutoConfiguration.imports}.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
