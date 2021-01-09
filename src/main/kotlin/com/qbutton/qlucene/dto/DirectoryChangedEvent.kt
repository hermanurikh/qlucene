package com.qbutton.qlucene.dto

import org.springframework.context.ApplicationEvent
import java.nio.file.Path
import java.nio.file.WatchEvent

/**
 * Event that either file or directory has been changed.
 */
sealed class InstanceChangedEvent(monitoredDir: Path) : ApplicationEvent(monitoredDir)

/**
 * Wrapper for events emitted by JDK WatchService.
 */
data class DirectoryChangedEvent(
    val monitoredDir: Path,
    val events: List<WatchEvent<Path>>,
    val filteredFiles: Set<String>?
) : InstanceChangedEvent(monitoredDir)

/**
 * Wrapper for file deletion event, emitted manually when inconsistency is found.
 */
data class FileDeletedEvent(val monitoredFile: Path) : InstanceChangedEvent(monitoredFile)
