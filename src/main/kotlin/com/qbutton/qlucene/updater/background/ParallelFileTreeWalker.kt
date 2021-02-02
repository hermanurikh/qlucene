package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FileValidator
import com.qbutton.qlucene.updater.FileUpdaterFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class ParallelFileTreeWalker(
    private val fileValidator: FileValidator,
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int,
) {

    /*
     Traverses the tree and adds files to index.
     The root is guaranteed to always be not in index. On some depth, there may be intersections - this is a field
     for potential optimisation not to traverse it.
     */
    fun walk(root: Path) {
        Files.walk(root, maxDepth)
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
