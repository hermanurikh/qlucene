package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class FileTreeWalker(
    private val watchService: WatchService,
    private val root: String
) : SimpleFileVisitor<Path>() {

    private val logger = LoggerFactory.getLogger(FileTreeWalker::class.java)

    override fun visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult {
        if (attr.isRegularFile) {
            watchService.registerFile(file.toString())
        } else {
            logger.info("irregular file on walker path: $file")
        }
        return FileVisitResult.CONTINUE
    }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        val stringPath = dir.toString()
        // root has already been visited by main method which starts walking
        if (stringPath == root) return FileVisitResult.CONTINUE

        val registrationResult = watchService.registerDir(stringPath, true)

        return if (registrationResult is DirectoryRegistrationSuccessful)
            FileVisitResult.CONTINUE
        else FileVisitResult.SKIP_SUBTREE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        logger.error("visit $file failed", exc)
        return FileVisitResult.CONTINUE
    }
}
