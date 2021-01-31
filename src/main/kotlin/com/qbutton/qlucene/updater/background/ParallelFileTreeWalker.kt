package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FileValidator
import com.qbutton.qlucene.updater.FileUpdaterFacade
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class ParallelFileTreeWalker(
    private val fileValidator: FileValidator,
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade,
) {

    fun walk(root: Path) {
        Files.walk(root)
            .parallel()
            .forEach {
                when {
                    Files.isRegularFile(it) -> visitFile(it.toAbsolutePath())
                }
            }
    }

    private fun visitFile(file: Path) {
        if (fileValidator.isValid(file)) {
            fileUpdaterFacade.update(fileIdConverter.toId(file))
        }
    }
}
