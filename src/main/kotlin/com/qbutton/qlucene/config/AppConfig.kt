package com.qbutton.qlucene.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
@PropertySource("classpath:search.properties")
class AppConfig @Autowired constructor(
    @Value("\${indexer.parallelism}")
    private val numThreads: Int
) {

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
    fun qLuceneExecutorService() = Executors.newFixedThreadPool(numThreads)!!
}
