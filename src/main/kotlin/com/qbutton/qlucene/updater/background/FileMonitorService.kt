package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.stereotype.Component

@Component
class FileMonitorService : MonitorService() {
    override fun isMonitored(path: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun register(path: String): RegistrationResult {
        TODO()
    }
}
