package com.project.SocialMediaFeedService.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
//Activate the async processing infrastructure.
//When you see @Async on a method, proxy that class and route calls to the thread pool.
public class AsyncConfig {

    //give name to your bean so that if there are multiple executor, Async will know which executor to target.
    @Bean(name = "backgroundTaskExecutor")
    public Executor backgroundTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(5); //Always 5 threads ready
        threadPoolTaskExecutor.setMaxPoolSize(20); //Max 20 under heavy load
        threadPoolTaskExecutor.setQueueCapacity(100); //Queue up to 100 waiting tasks
        threadPoolTaskExecutor.setThreadNamePrefix("fanout-thread-"); //Identifiable in logs
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
    }
}
