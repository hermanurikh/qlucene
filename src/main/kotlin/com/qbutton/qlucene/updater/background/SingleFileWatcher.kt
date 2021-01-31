package com.qbutton.qlucene.updater.background

import io.methvin.watcher.DirectoryChangeEvent
import org.apache.log4j.helpers.FileWatchdog
import org.springframework.context.ApplicationEventPublisher
import java.nio.file.Path

class SingleFileWatcher(
    private val path: Path,
    private val applicationEventPublisher: ApplicationEventPublisher,
    pollingDelay: Long
) : FileWatchdog(path.toString()) {

    init {
        setDelay(pollingDelay)
    }

    override fun doOnChange() {
        // this actually can be null - parent class calls this method in constructor, and this field is not set then
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(
                DirectoryChangeEvent(
                    DirectoryChangeEvent.EventType.MODIFY,
                    false,
                    path,
                    null,
                    1,
                    path
                )
            )
        }
    }
}
