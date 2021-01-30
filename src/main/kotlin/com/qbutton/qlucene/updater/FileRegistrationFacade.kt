package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.AbnormalFileRegistrationResult
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.FileNotFoundRegistrationResult
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.updater.background.ParallelFileTreeWalker
import com.qbutton.qlucene.updater.background.WatchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class FileRegistrationFacade @Autowired constructor(
    private val watchService: WatchService,
    private val parallelFileTreeWalker: ParallelFileTreeWalker,
) {

    fun registerPath(path: String): RegistrationResult {
        val file = Paths.get(path).toFile()
        return when {
            !file.exists() -> FileNotFoundRegistrationResult(path)
            file.isFile -> {
                watchService.registerFile(path)
            }
            file.isDirectory -> registerRootDir(path)
            else -> AbnormalFileRegistrationResult(path)
        }
    }

    /**
     * Registers current directory and recursively walks the file tree to register untracked directories.
     * It is limited by depth passed as class parameter.
     */
    private fun registerRootDir(path: String): RegistrationResult {
        val result = watchService.registerDir(path, true)
        if (result !is DirectoryAlreadyRegistered) {
            val currDir = Paths.get(path)
            parallelFileTreeWalker.walk(currDir)
        }
        return result
    }
}
