package com.qbutton.qlucene.dto

import org.springframework.context.ApplicationEvent
import java.nio.file.Path
import java.nio.file.WatchEvent

data class FileChangedEvent(val monitoredFile: Path, val events: List<WatchEvent<Path>>) : ApplicationEvent(monitoredFile)