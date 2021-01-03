package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.RegistrationResult
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractWatchService constructor(
    private val fileIdConverter: FileIdConverter,
) {
    private val monitoredDirectories = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun tryMonitor(path: String): Boolean {
        val dirId = fileIdConverter.toId(path)
        return monitoredDirectories.add(dirId)
    }

    abstract fun register(path: String): RegistrationResult
}
