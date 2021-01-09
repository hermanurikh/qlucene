package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DirectoryChangedEvent
import com.qbutton.qlucene.updater.FileUpdaterFacade
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

/**
 * Asynchronously (see AppConfig.java) listens to events emitted by files or directories changed and takes corresponding actions.
 */
@Component
class BackgroundEventsListener @Autowired constructor(
    private val userAPI: UserAPI,
    private val watchService: WatchService,
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade
) {

    private val logger = LoggerFactory.getLogger(BackgroundEventsListener::class.java)!!

    @EventListener
    fun listen(directoryChangedEvent: DirectoryChangedEvent) {
        for (event in directoryChangedEvent.events) {
            val kind = event.kind()

            // suppressing - it is like that even in official Oracle documentation :|
            @Suppress("UNCHECKED_CAST")
            val changedEntryName = event.context()
            val resolvedEntry = directoryChangedEvent.monitoredDir.resolve(changedEntryName)
            val path = resolvedEntry.toString()
            val entryId = fileIdConverter.toId(path)

            if (shouldFilterOut(path, directoryChangedEvent)) {
                logger.info("path $path is not monitored, skipping event $kind")
                continue
            }

            when (kind) {
                ENTRY_CREATE -> {
                    // for directory or file, register it
                    logger.info("CREATE event detected for $path")
                    userAPI.addToIndex(path)
                }
                ENTRY_MODIFY -> {
                    // for file, update it
                    if (Files.isRegularFile(resolvedEntry)) {
                        logger.info("MODIFY event detected for $path")
                        fileUpdaterFacade.update(entryId)
                    }
                    // for directory, do nothing -> adding/removing file will trigger other 2 events
                }
                ENTRY_DELETE -> {
                    logger.info("DELETE event detected for $path")
                    watchService.unregister(path)
                }
            }
        }
    }

    /**
     * Filters out events. Sometimes we monitor only specific files in given dir, meaning that events corresponding
     * to other files should be skipped.
     */
    private fun shouldFilterOut(path: String, directoryChangedEvent: DirectoryChangedEvent): Boolean {
        return directoryChangedEvent.filteredFiles != null && !directoryChangedEvent.filteredFiles.contains(path)
    }
}
