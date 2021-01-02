package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import org.springframework.stereotype.Component

@Component
class DirectoryMonitorService : MonitorService() {
    // all file changed should be delegated to file monitor service
    override fun isMonitored(path: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun register(path: String): DirectoryRegistrationSuccessful {
        TODO()
    }
}
