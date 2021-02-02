package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.updater.background.io.methvin.watcher.DirectoryWatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import javax.annotation.PreDestroy

@Component
class WatchService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${file.polling-interval}")
    private val filePollingInterval: Long,
    private val qLuceneExecutorService: ExecutorService
) : Resettable {

    private val logger = LoggerFactory.getLogger(WatchService::class.java)
    private val directoryWatchers = Collections.newSetFromMap(ConcurrentHashMap<DirectoryWatcher, Boolean>())

    fun attachWatcherToFile(path: Path) {
        val watcher = SingleFileWatcher(path, applicationEventPublisher, filePollingInterval)
        qLuceneExecutorService.submit(watcher)
    }

    fun attachWatcherToDir(path: Path): Future<*> =
        qLuceneExecutorService.submit {
            // this is recursive and takes a while, do it async
            val watcher = DirectoryWatcher.builder()
                .path(path)
                .listener { applicationEventPublisher.publishEvent(it) }
                .build()
            directoryWatchers.add(watcher)
            watcher.watchAsync(qLuceneExecutorService)
        }

    override fun resetState() {
        directoryWatchers.forEach { it.close() }
        directoryWatchers.clear()
    }

    @PreDestroy
    fun stopWatchers() {
        logger.info("stopping watchers")
        directoryWatchers.forEach { it.close() }
    }
}
