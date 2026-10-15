package com.yourbrand.chat.config;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Runs streaming replies off the request thread; virtual threads make blocking I/O cheap. */
    @Bean(destroyMethod = "close")
    ExecutorService chatStreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
