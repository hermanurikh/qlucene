package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.FileRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.nio.file.WatchService


@Component
class FileWatchService @Autowired constructor(
    watchService: WatchService,
    fileIdConverter: FileIdConverter,
    applicationEventPublisher: ApplicationEventPublisher
) : AbstractWatchService(watchService, fileIdConverter, applicationEventPublisher) {

    override fun register(path: String): RegistrationResult {
        if (!tryMonitor(path)) {
            return FileAlreadyRegistered(path)
        }

        attachWatcher(path)
        return FileRegistrationSuccessful(path)
    }
}
