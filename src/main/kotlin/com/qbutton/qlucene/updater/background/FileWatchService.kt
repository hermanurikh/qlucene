package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.FileRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FileWatchService @Autowired constructor(
    fileIdConverter: FileIdConverter,
    private val backgroundEventsPublisher: BackgroundEventsPublisher,
) : AbstractWatchService(fileIdConverter) {

    override fun register(path: String): RegistrationResult {
        if (!tryMonitor(path)) {
            return FileAlreadyRegistered(path)
        }

        backgroundEventsPublisher.attachWatcher(path)
        return FileRegistrationSuccessful(path)
    }
}
