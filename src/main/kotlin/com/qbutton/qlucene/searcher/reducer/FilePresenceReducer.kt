package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.FileDeletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

/**
 * If a file is no more present in the system (due to enclosing directory removal), this should filter it out.
 * See WatchService::unregister for details.
 */
@Component
@Order(1)
class FilePresenceReducer(
    private val fileIdConverter: FileIdConverter,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Reducer() {

    override fun reduce(documents: List<DocumentSearchResult>) = documents.filter {
        val stringPath = fileIdConverter.toPath(it.fileId)
        val path = Paths.get(stringPath)
        val fileExists = Files.exists(path)
        if (!fileExists) {
            // emit event to refresh state for this file
            applicationEventPublisher.publishEvent(FileDeletedEvent(path))
        }
        fileExists
    }
}
