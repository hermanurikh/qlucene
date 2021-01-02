package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.dto.RegistrationResult

abstract class MonitorService {
    abstract fun isMonitored(path: String): Boolean

    abstract fun register(path: String): RegistrationResult
}