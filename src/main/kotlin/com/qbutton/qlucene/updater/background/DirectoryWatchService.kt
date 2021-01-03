package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes

@Component
class DirectoryWatchService @Autowired constructor(
    watchService: WatchService,
    fileIdConverter: FileIdConverter,
    applicationEventPublisher: ApplicationEventPublisher,
    private val fileMonitorService: FileWatchService,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int
) : AbstractWatchService(watchService, fileIdConverter, applicationEventPublisher) {

    /**
     * Registers current directory and recursively walks the file tree to visit untracked directories.
     * It is limited by depth passed as class parameter.
     */
    override fun register(path: String): RegistrationResult {

        if (!tryMonitor(path)) {
            return DirectoryAlreadyRegistered(path)
        }

        attachWatcher(path)
        val fileTreeWalker = FileTreeWalker(fileMonitorService, this)
        Files.walkFileTree(Paths.get(path), emptySet(), maxDepth, fileTreeWalker)

        return DirectoryRegistrationSuccessful(path)
    }
}

private class FileTreeWalker(
    private val fileMonitorService: FileWatchService,
    private val directoryMonitorService: DirectoryWatchService
) : SimpleFileVisitor<Path>() {

    override fun visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult {
        if (attr.isRegularFile) {
            fileMonitorService.register(file.toString())
        } else {
            TODO("log")
        }
        return FileVisitResult.CONTINUE
    }

    // Print each directory visited.
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (!directoryMonitorService.tryMonitor(dir.toString())) {
            return FileVisitResult.SKIP_SUBTREE
        }
        directoryMonitorService.attachWatcher(dir.toString())
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        TODO("log")
        return FileVisitResult.CONTINUE
    }
}
