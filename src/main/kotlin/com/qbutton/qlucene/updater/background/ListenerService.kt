package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.FileChangedEvent
import com.qbutton.qlucene.updater.UpdaterFacade
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW

@Component
class ListenerService @Autowired constructor(
    private val updaterFacade: UpdaterFacade,
    private val fileIdConverter: FileIdConverter,
    private val userAPI: UserAPI
) {

    private val logger = LoggerFactory.getLogger(ListenerService::class.java)!!

    /**
     * Listens to events emitted by files or directories changed and takes corresponding actions.
     */
    @EventListener
    fun listen(fileChangedEvent: FileChangedEvent) {
        for (event in fileChangedEvent.events) {
            val kind = event.kind()
            if (kind === OVERFLOW) {
                logger.error("Overflow event $event received for file ${fileChangedEvent.monitoredFile}")
                continue
            }

            //suppressing - it is like that even in official Oracle documentation :|
            @Suppress("UNCHECKED_CAST")
            val changedEntryName = event.context()
            val resolvedEntry = fileChangedEvent.monitoredFile.resolve(changedEntryName)
            val entryId = fileIdConverter.toId(resolvedEntry.toString())

            when (kind) {
                ENTRY_CREATE -> {
                    //for directory or file, register it
                    userAPI.addToIndex(resolvedEntry.toString())
                }
                ENTRY_MODIFY -> {
                    // for file, update it
                    if (Files.isRegularFile(resolvedEntry)) {
                        updaterFacade.update(resolvedEntry.toString())
                    }
                    // for directory, do nothing -> adding/removing file will trigger other 2 events
                    // TODO validate this theory
                }
                ENTRY_DELETE -> {
                    // for file, update it to empty contents
                    if (Files.isRegularFile(resolvedEntry)) {
                        updaterFacade.update(resolvedEntry.toString())
                    }
                    // TODO("for directory, we need to walk file tree")

                }
            }
        }
    }
}