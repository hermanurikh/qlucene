package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.FileRegistrationSuccessful
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.updater.FileUpdaterFacade
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A class to register directories or files for monitoring.
 *
 * A monitored directory can be in 2 states:
 *      - only first level (some or all) of the files are monitored. This happens when user registers a single file.
 *      Java's WatchService can only watch directories, so when we register file, we actually register parent dir and
 *      filter events which check only needed file.
 *      - whole directory tree (limited by max-depth parameter) is monitored. This happens when user registers whole dir.
 */
@Component
class WatchService @Autowired constructor(
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade,
    private val backgroundEventsPublisher: BackgroundEventsPublisher,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int
) {
    // stores mapping between currently monitored dir and state (monitored recursively or one-level). See class doc.
    private val monitoredDirectories = HashMap<String, Boolean>()
    private val dirLocks = ConcurrentHashMap<String, Lock>()
    private val monitoredFiles = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val logger = LoggerFactory.getLogger(WatchService::class.java)

    /**
     * Attempts to register a directory, completely or to monitor specific file.
     * If a directory is not registered already, registers it as needed.
     * Otherwise:
     *      - if it is registered non-completely, it should have some specific files to monitor.
     *          - if we need to register it completely now, upgrade it by removing specific files
     *          - if we also need non-completely, we need to add specific files. Add the file to the list.
     *      - if it is registered completely, it already includes everything needed, skip.
     *
     * When we are registering some internal dir, some files may already be registered. This means watcher is already
     * attached to dir. We need to remove filters and go deeper (skip siblings).
     *
     * We need a lock because many threads may be calling it simultaneously, and operations inside use CAS and are not atomic.
     */
    fun registerDir(path: String, shouldBeCompletelyMonitored: Boolean, fileToMonitor: String? = null): RegistrationResult {

        val lock = dirLocks.computeIfAbsent(path) { ReentrantLock() }
        try {
            lock.lock()
            val currValue = monitoredDirectories[path]
            if (shouldBeCompletelyMonitored) {
                return if (currValue != null && currValue) {
                    // if we need complete monitoring and mapping is there and already complete, do nothing
                    DirectoryAlreadyRegistered(path)
                } else if (currValue == null) {
                    // if we need complete monitoring and mapping is not there, put value and attach watcher
                    monitoredDirectories[path] = true
                    backgroundEventsPublisher.attachWatcher(path)
                    DirectoryRegistrationSuccessful(path)
                } else {
                    // if we need complete monitoring and current is non-complete, update value and remove specific mappings
                    monitoredDirectories[path] = true
                    backgroundEventsPublisher.clearFilteredFiles(path)
                    DirectoryRegistrationSuccessful(path)
                }
            } else {
                return if (currValue != null && currValue) {
                    // if we don't need complete monitoring, but it is already complete, do nothing
                    DirectoryAlreadyRegistered(path)
                } else if (currValue == null) {
                    // if we need incomplete monitoring and mapping is not there, put value, attach watcher and add files
                    monitoredDirectories[path] = false
                    backgroundEventsPublisher.updateFilteredFiles(path, fileToMonitor!!)
                    backgroundEventsPublisher.attachWatcher(path)
                    DirectoryRegistrationSuccessful(path)
                } else {
                    // if we need incomplete monitoring and current is non-complete, update specific mapping
                    backgroundEventsPublisher.updateFilteredFiles(path, fileToMonitor!!)
                    DirectoryAlreadyRegistered(path)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Registers current directory and recursively walks the file tree to register untracked directories.
     * It is limited by depth passed as class parameter.
     */
    fun registerRootDir(path: String): RegistrationResult {
        val result = registerDir(path, true)
        if (result !is DirectoryAlreadyRegistered) {
            val fileTreeWalker = FileTreeWalker(this, path)
            Files.walkFileTree(Paths.get(path), emptySet(), maxDepth, fileTreeWalker)
        }
        return result
    }

    fun registerFile(path: String): RegistrationResult {
        if (!tryMonitorFile(path)) {
            return FileAlreadyRegistered(path)
        }
        logger.info("registering file $path")

        val parentDir = Paths.get(path).parent
        registerDir(parentDir.toString(), false, path)

        logger.info("adding file $path to index")

        fileUpdaterFacade.update(fileIdConverter.toId(path))
        return FileRegistrationSuccessful(path)
    }

    private fun tryMonitorFile(path: String): Boolean {
        val fileId = fileIdConverter.toId(path)
        return monitoredFiles.add(fileId)
    }
}
