package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Component
class FileMonitorService @Autowired constructor(
    private val fileIdConverter: FileIdConverter
) : MonitorService() {

    private val monitoredFiles = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    override fun tryMonitor(path: String): Boolean {
        val fileId = fileIdConverter.toId(path)
        return monitoredFiles.add(fileId)
    }

    override fun register(path: String): RegistrationResult {
        if (!tryMonitor(path)) {
            return FileAlreadyRegistered(path)
        }

        TODO("attach watcher")
    }
}
