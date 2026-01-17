package com.github.myrrhax.notification_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    public Executor emailTaskExecutor(@Value("${app.concurrent.email-executor-thread-count}") int threadCount) {
        return Executors.newFixedThreadPool(threadCount);
    }
}
