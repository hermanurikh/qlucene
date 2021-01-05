package com.qbutton.qlucene.dto

import org.springframework.context.ApplicationEvent
import java.nio.file.Path
import java.nio.file.WatchEvent

data class DirectoryChangedEvent(
    val monitoredDir: Path,
    val events: List<WatchEvent<Path>>,
    val filteredFiles: Set<String>?
) : ApplicationEvent(monitoredDir)
