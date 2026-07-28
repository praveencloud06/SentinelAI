package com.sentinelai.knowledge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async processing, specifically for embedding generation.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Value("${sentinelai.knowledge.embedding.async.thread-pool-size:5}")
    private int threadPoolSize;
    
    @Value("${sentinelai.knowledge.embedding.async.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${sentinelai.knowledge.embedding.async.thread-name-prefix:embedding-async-}")
    private String threadNamePrefix;
    
    @Bean(name = "embeddingTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        log.info("Configuring embedding task executor with pool size: {}, queue capacity: {}", 
                threadPoolSize, queueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async method execution error in method: {}", method.getName(), throwable);
            log.error("Parameters: {}", java.util.Arrays.toString(params));
        };
    }
}