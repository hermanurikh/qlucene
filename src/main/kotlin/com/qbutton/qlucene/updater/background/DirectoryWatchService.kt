package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

@Component
class DirectoryWatchService @Autowired constructor(
    fileIdConverter: FileIdConverter,
    private val fileMonitorService: FileWatchService,
    private val backgroundEventsPublisher: BackgroundEventsPublisher,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int
) : AbstractWatchService(fileIdConverter) {

    /**
     * Registers current directory and, if needed, recursively walks the file tree to register untracked directories.
     * It is limited by depth passed as class parameter.
     */
    override fun register(path: String) = register(path, true)

    fun register(path: String, recursive: Boolean): RegistrationResult {
        if (!tryMonitor(path)) {
            return DirectoryAlreadyRegistered(path)
        }

        backgroundEventsPublisher.attachWatcher(path)
        if (recursive) {
            val fileTreeWalker = FileTreeWalker(fileMonitorService, this)
            Files.walkFileTree(Paths.get(path), emptySet(), maxDepth, fileTreeWalker)
        }

        return DirectoryRegistrationSuccessful(path)
    }
}

private class FileTreeWalker(
    private val fileMonitorService: FileWatchService,
    private val directoryMonitorService: DirectoryWatchService
) : SimpleFileVisitor<Path>() {

    private val logger = LoggerFactory.getLogger(FileTreeWalker::class.java)

    override fun visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult {
        if (attr.isRegularFile) {
            fileMonitorService.register(file.toString())
        } else {
            logger.info("irregular file on walker path: $file")
        }
        return FileVisitResult.CONTINUE
    }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        val registrationResult = directoryMonitorService.register(dir.toString(), false)

        return if (registrationResult is DirectoryRegistrationSuccessful)
            FileVisitResult.CONTINUE
        else FileVisitResult.SKIP_SUBTREE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        logger.error("visit $file failed", exc)
        return FileVisitResult.CONTINUE
    }
}
