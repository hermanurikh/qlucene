package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.Resettable
import io.methvin.watcher.DirectoryWatcher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool

@Component
class WatchService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${file.polling-interval}")
    private val filePollingInterval: Long
) : Resettable {

    private val directoryWatchers = Collections.newSetFromMap(ConcurrentHashMap<DirectoryWatcher, Boolean>())

    fun attachWatcherToFile(path: Path) {
        val watcher = SingleFileWatcher(path, applicationEventPublisher, filePollingInterval)
        ForkJoinPool.commonPool().submit(watcher)
    }

    fun attachWatcherToDir(path: Path) {
        val watcher = DirectoryWatcher.builder()
            .path(path)
            .listener { applicationEventPublisher.publishEvent(it) }
            .build()
        directoryWatchers.add(watcher)
        watcher.watchAsync()
    }

    override fun resetState() {
        directoryWatchers.forEach { it.close() }
        directoryWatchers.clear()
    }
}
