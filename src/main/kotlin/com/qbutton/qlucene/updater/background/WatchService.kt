package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.Locker
import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.DirectoryShouldNotBeRegistered
import com.qbutton.qlucene.dto.DirectoryUnregistrationSuccessful
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.FileMonitorState
import com.qbutton.qlucene.dto.FileRegistrationSuccessful
import com.qbutton.qlucene.dto.FileSizeExceedsLimits
import com.qbutton.qlucene.dto.FileUnregistrationSuccessful
import com.qbutton.qlucene.dto.NotRegistered
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.dto.UnregistrationResult
import com.qbutton.qlucene.updater.FileUpdaterFacade
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

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
    private val locker: Locker,
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade,
    private val backgroundEventsPublisher: BackgroundEventsPublisher,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int,
    @Value("\${indexed-contents.root-dir}")
    private val storageIndexDir: String,
    @Value("\${file.max-indexed-size}")
    private val maxFileSize: Long
) : Resettable {
    // stores mapping between currently monitored dir/file and state (monitored recursively or one-level). See class doc.
    private val monitoredFiles = ConcurrentHashMap<String, FileMonitorState>()
    private val logger = LoggerFactory.getLogger(WatchService::class.java)
    private val indexStoragePath = Paths.get(storageIndexDir)

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
        if (Files.isSameFile(indexStoragePath, Paths.get(path))) {
            // don't attempt to register files in directory storing index, otherwise it may be endless recursion
            return DirectoryShouldNotBeRegistered(path)
        }
        val dirId = fileIdConverter.toId(path)
        try {
            locker.lockId(dirId)
            val currValue = monitoredFiles[dirId]
            if (shouldBeCompletelyMonitored) {
                return when {
                    // if we need complete monitoring and mapping is there and already complete, do nothing
                    currValue != null && currValue.isMonitoredCompletely -> DirectoryAlreadyRegistered(path)
                    // if we need complete monitoring and mapping is not there, put value and attach watcher
                    currValue == null -> {
                        monitoredFiles[dirId] = FileMonitorState(isDirectory = true, isMonitoredCompletely = true)
                        backgroundEventsPublisher.attachWatcher(path)
                        DirectoryRegistrationSuccessful(path)
                    }
                    else -> {
                        // if we need complete monitoring and current is non-complete, update value and remove specific mappings
                        monitoredFiles[dirId] = FileMonitorState(isDirectory = true, isMonitoredCompletely = true)
                        backgroundEventsPublisher.clearFilteredFiles(path)
                        DirectoryRegistrationSuccessful(path)
                    }
                }
            } else {
                return when {
                    // if we don't need complete monitoring, but it is already complete, do nothing
                    currValue != null && currValue.isMonitoredCompletely -> DirectoryAlreadyRegistered(path)
                    // if we need incomplete monitoring and mapping is not there, put value, attach watcher and add files
                    currValue == null -> {
                        monitoredFiles[dirId] = FileMonitorState(isDirectory = true, isMonitoredCompletely = false)
                        backgroundEventsPublisher.updateFilteredFiles(path, fileToMonitor!!)
                        backgroundEventsPublisher.attachWatcher(path)
                        DirectoryRegistrationSuccessful(path)
                    }
                    else -> {
                        // if we need incomplete monitoring and current is non-complete, update specific mapping
                        backgroundEventsPublisher.updateFilteredFiles(path, fileToMonitor!!)
                        DirectoryAlreadyRegistered(path)
                    }
                }
            }
        } finally {
            locker.unlockId(dirId)
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

    fun unregister(path: String): UnregistrationResult {
        val fileId = fileIdConverter.toId(path)

        try {
            locker.lockId(fileId)
            // remove the mapping immediately, so that later they could be re-registered
            val monitorState = monitoredFiles.remove(fileId) ?: return NotRegistered(path)
            return if (monitorState.isDirectory) {
                /*
                For directory deletion, it is not obvious how to restore which files were there - we can't walk it as it has already been deleted.
                I don't want to cache file system tree to walk it on directory deletion as it looks fragile and excessive.

                So I decided to go with lazy approach: the files which are no longer there due to directory deletion are filtered out on search,
                and we also push an event to exclude this file from indices.
                 */
                DirectoryUnregistrationSuccessful(path)
            } else {
                // for file, just update it to empty contents
                fileUpdaterFacade.update(fileId)
                FileUnregistrationSuccessful(path)
            }
        } finally {
            locker.unlockId(fileId)
        }
    }

    fun registerFile(path: String): RegistrationResult {
        if (Files.size(Paths.get(path)) > maxFileSize) {
            return FileSizeExceedsLimits(path)
        }
        val fileId = fileIdConverter.toId(path)
        try {
            locker.lockId(fileId)
            if (!tryMonitorFile(fileId)) {
                return FileAlreadyRegistered(path)
            }
            logger.info("registering file $path")

            val parentDir = Paths.get(path).parent
            registerDir(parentDir.toString(), false, path)

            logger.info("adding file $path to index")

            fileUpdaterFacade.update(fileIdConverter.toId(path))
            return FileRegistrationSuccessful(path)
        } finally {
            locker.unlockId(fileId)
        }
    }

    private fun tryMonitorFile(fileId: String): Boolean {
        val additionResult = monitoredFiles.putIfAbsent(
            fileId,
            FileMonitorState(isDirectory = false, isMonitoredCompletely = false)
        )
        return additionResult == null
    }

    override fun resetState() {
        monitoredFiles.clear()
    }
}
