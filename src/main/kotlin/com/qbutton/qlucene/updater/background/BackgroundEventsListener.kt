package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FileValidator
import com.qbutton.qlucene.common.FilteredOutRoots
import com.qbutton.qlucene.updater.FileUpdaterFacade
import io.methvin.watcher.DirectoryChangeEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Asynchronously (see AppConfig.java) listens to events emitted by files or directories changed and takes corresponding actions.
 */
@Component
class BackgroundEventsListener @Autowired constructor(
    private val fileValidator: FileValidator,
    private val fileIdConverter: FileIdConverter,
    private val filteredOutRoots: FilteredOutRoots,
    private val fileUpdaterFacade: FileUpdaterFacade,
    private val parallelFileTreeWalker: ParallelFileTreeWalker,
) {

    private val logger = LoggerFactory.getLogger(BackgroundEventsListener::class.java)!!

    @EventListener
    fun listen(event: DirectoryChangeEvent) {
        val path = event.path().toAbsolutePath()
        if (filteredOutRoots.shouldFilterOut(path.toString())) {
            return
        }

        val isFile = !event.isDirectory
        val fileId = fileIdConverter.toId(path)

        logger.info("${event.eventType()} event detected for $path")
        if (isFile) {
            // for file, whether it's create, delete or modify, we need to just update contents in index
            if (fileValidator.isValid(path)) {
                fileUpdaterFacade.update(fileId)
            }
        } else {
            // for directory, we only need to do something on CREATE.
            // MODIFY will trigger file events, and DELETE happens lazily (see FilePresenceReducer)
            if (event.eventType() == DirectoryChangeEvent.EventType.CREATE) {
                parallelFileTreeWalker.walk(path)
            }
        }
    }
}
