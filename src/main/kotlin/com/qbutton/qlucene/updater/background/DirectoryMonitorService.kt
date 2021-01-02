package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Component
class DirectoryMonitorService @Autowired constructor(
    private val fileMonitorService: FileMonitorService,
    private val fileIdConverter: FileIdConverter,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int
) : MonitorService() {
    private val monitoredDirectories = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    // all file changed should be delegated to file monitor service
    override fun tryMonitor(path: String): Boolean {
        val dirId = fileIdConverter.toId(path)
        return monitoredDirectories.add(dirId)
    }

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

    /**
     * This is just to add directory to background monitoring, without walking in depth.
     */
    fun attachWatcher(path: String) {

        TODO("just attach")
    }
}

private class FileTreeWalker(
    private val fileMonitorService: FileMonitorService,
    private val directoryMonitorService: DirectoryMonitorService
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
