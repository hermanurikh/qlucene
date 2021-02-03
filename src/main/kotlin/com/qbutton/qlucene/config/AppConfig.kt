package com.qbutton.qlucene.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
@EnableScheduling
@PropertySource("classpath:search.properties")
class AppConfig {

    /**
     * This bean ensures spring processes events in async manner.
     */
    @Bean
    fun applicationEventMulticaster(qLuceneExecutorService: ExecutorService): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        eventMulticaster.setTaskExecutor(qLuceneExecutorService)
        return eventMulticaster
    }

    @Bean
    fun qLuceneExecutorService(
        @Value("\${indexer.parallelism}") numThreads: Int
    ) = Executors.newFixedThreadPool(numThreads)!!

    @Bean
    fun propertyPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
        return PropertySourcesPlaceholderConfigurer()
    }

    @Bean
    fun taskScheduler(): TaskScheduler {
        return ConcurrentTaskScheduler()
    }
}
