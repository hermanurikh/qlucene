package com.qbutton.qlucene

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:search.properties")
@SpringBootApplication
class QLuceneApplication

fun main(args: Array<String>) {
    runApplication<QLuceneApplication>(*args)
}
