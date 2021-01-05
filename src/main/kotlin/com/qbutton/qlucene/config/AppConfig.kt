package com.qbutton.qlucene.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import java.nio.file.FileSystems

@Configuration
@PropertySource("classpath:search.properties")
class AppConfig {

    @Bean
    fun jdkWatchService() = FileSystems.getDefault().newWatchService()!!

    /**
     * This bean ensures spring processes events in async manner.
     */
    @Bean
    fun applicationEventMulticaster(): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        eventMulticaster.setTaskExecutor(SimpleAsyncTaskExecutor())
        return eventMulticaster
    }
}
