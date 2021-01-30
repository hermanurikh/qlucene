package com.qbutton.qlucene.updater.background

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class ParallelFileTreeWalker(
    private val watchService: WatchService,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int,
) {

    fun walk(root: Path) {
        Files.walk(root, maxDepth)
            .parallel()
            .forEach {
                when {
                    Files.isDirectory(it) -> visitDirectory(it)
                    else -> visitFile(it)
                }
            }
    }

    private val logger = LoggerFactory.getLogger(ParallelFileTreeWalker::class.java)

    private fun visitFile(file: Path) {
        if (Files.isRegularFile(file)) {
            watchService.registerFile(file.toString())
        } else {
            logger.info("irregular file on walker path: $file")
        }
    }

    private fun visitDirectory(dir: Path) {
        val stringPath = dir.toString()

        watchService.registerDir(stringPath, true)
    }
}
