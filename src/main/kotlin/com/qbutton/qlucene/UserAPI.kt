package com.qbutton.qlucene

import com.qbutton.qlucene.dto.AbnormalFileRegistrationResult
import com.qbutton.qlucene.dto.FileNotFoundRegistrationResult
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.searcher.SearchFacade
import com.qbutton.qlucene.updater.background.DirectoryMonitorService
import com.qbutton.qlucene.updater.background.FileMonitorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Paths

/**
 * A class exposing all API via which a user interacts with application.
 */
@Component
class UserAPI @Autowired constructor(
    private val searchFacade: SearchFacade,
    private val directoryMonitorService: DirectoryMonitorService,
    private val fileMonitorService: FileMonitorService
) {
    fun addToIndex(path: String): RegistrationResult {
        val file = Paths.get(path).toFile()

        return when {
            !file.exists() -> FileNotFoundRegistrationResult(path)
            file.isDirectory -> {
                directoryMonitorService.register(path)
            }
            file.isFile -> {
                fileMonitorService.register(path)
            }
            else -> AbnormalFileRegistrationResult(path)
        }
    }

    fun search(term: Term) = searchFacade.search(term)
}
