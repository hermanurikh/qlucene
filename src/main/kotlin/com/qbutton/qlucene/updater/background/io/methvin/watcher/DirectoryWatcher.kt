package com.qbutton.qlucene.updater.background.io.methvin.watcher

import com.sun.nio.file.ExtendedWatchEventModifier
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.hashing.FileHash
import io.methvin.watcher.hashing.FileHasher
import io.methvin.watchservice.MacOSXListeningWatchService
import io.methvin.watchservice.WatchablePath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.Collections
import java.util.SortedMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executor

/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/*
This is a copy of io.methvin.watcher.DirectoryWatcher, with a couple of small patches:
 - allowing the library to break in the middle of traversing file path
 - adding max depth of traversal
 - simplification of structure for my use case
 */

class DirectoryWatcher(
    path: Path,
    isTraversalCancelledForId: (String) -> Boolean,
    toFileIdAction: (Path) -> String,
    listener: DirectoryChangeListener,
    registeredFileIds: MutableSet<String>,
    indexFileAction: (Path) -> Unit,
    private val maxDepth: Int
) {

    private val logger: Logger
    private val watchService: WatchService
    private val registeredPathToRootPath: MutableMap<Path?, Path?>
    private val isMac: Boolean
    private val listener: DirectoryChangeListener
    private val pathHashes: SortedMap<Path?, FileHash?>
    private val directories: MutableSet<Path?>
    private val keyRoots: MutableMap<WatchKey, Path?>
    // set to null until we check if FILE_TREE is supported
    private var fileTreeSupported: Boolean? = null

    private val fileHasher: FileHasher?
    @Volatile
    var isClosed = false
        private set

    init {
        registeredPathToRootPath = HashMap()
        this.listener = listener
        this.watchService = osDefaultWatchService()
        isMac = watchService is MacOSXListeningWatchService
        pathHashes = ConcurrentSkipListMap()
        directories = Collections.newSetFromMap(ConcurrentHashMap())
        keyRoots = ConcurrentHashMap()
        this.fileHasher = FileHasher.DEFAULT_FILE_HASHER
        this.logger = LoggerFactory.getLogger(DirectoryWatcher::class.java)
        PathUtils.initWatcherState(
            path,
            fileHasher,
            pathHashes,
            directories,
            maxDepth,
            registeredFileIds,
            toFileIdAction,
            isTraversalCancelledForId,
            indexFileAction
        )
        registerAll(path, path)
    }

    private fun osDefaultWatchService(): WatchService {
        val isMac = System.getProperty("os.name").toLowerCase().contains("mac")
        return if (isMac) {
            MacOSXListeningWatchService(
                object : MacOSXListeningWatchService.Config {
                    override fun fileHasher(): FileHasher? {
                        /**
                         * Always return null here. When MacOSXListeningWatchService is used with
                         * DirectoryWatcher, then the hashing should happen within DirectoryWatcher. If
                         * users wish to override this then they must instantiate
                         * MacOSXListeningWatchService and pass it to DirectoryWatcher.
                         */
                        return null
                    }
                }
            )
        } else {
            FileSystems.getDefault().newWatchService()
        }
    }

    /**
     * Asynchronously watch the directories.
     *
     * @param executor the executor to use to watch asynchronously
     */
    fun watchAsync(executor: Executor): CompletableFuture<Void> {
        return CompletableFuture.supplyAsync(
            {
                watch()
                null
            },
            executor
        )
    }

    /**
     * Watch the directories. Block until either the listener stops watching or the DirectoryWatcher
     * is closed.
     */
    private fun watch() {
        while (listener.isWatching) {
            // wait for key to be signalled
            val key: WatchKey = try {
                watchService.take()
            } catch (x: InterruptedException) {
                return
            }
            for (event in key.pollEvents()) {
                try {
                    val kind = event.kind()
                    // Context for directory entry event is the file name of entry
                    val ev = PathUtils.cast<Path>(event)
                    val count = ev!!.count()
                    val eventPath = ev.context()
                    check(keyRoots.containsKey(key)) { "WatchService returned key [$key] but it was not found in keyRoots!" }
                    val registeredPath = keyRoots[key]
                    val rootPath = registeredPathToRootPath[registeredPath]
                    val childPath = if (eventPath == null) null else keyRoots[key]!!.resolve(eventPath)
                    /*
           * If a directory is created, and we're watching recursively, then register it
           * and its sub-directories.
           */if (kind === StandardWatchEventKinds.OVERFLOW) {
                        onEvent(DirectoryChangeEvent.EventType.OVERFLOW, false, childPath, count, rootPath)
                    } else // hashing is enabled, so delete the hashes
                    // this will remove from the original map
// hashing is disabled, so just notify on the path we got the event for
                    /*
                                       * Note that existingHash may be null due to the file being created before we
                                       * start listening It's important we don't discard the event in this case
                                       */

                    /*
                         * newHash can be null when using File#delete() on windows - it generates MODIFY
                         * and DELETE in succession. In this case the MODIFY event can be safely ignored
                         *//*
               * Our custom Mac service sends subdirectory changes but the Windows/Linux do
               * not. Walk the file tree to make sure we send create events for any files that
               * were created.
               */ checkNotNull(eventPath) { "WatchService returned a null path for " + kind.name() }
                    if (kind === StandardWatchEventKinds.ENTRY_CREATE) {
                        val isDirectory = Files.isDirectory(childPath, LinkOption.NOFOLLOW_LINKS)
                        if (isDirectory) {
                            if (true != fileTreeSupported) {
                                registerAll(childPath, rootPath)
                            }
                            /*
                   * Our custom Mac service sends subdirectory changes but the Windows/Linux do
                   * not. Walk the file tree to make sure we send create events for any files that
                   * were created.
                   */if (!isMac) {
                                PathUtils.recursiveVisitFiles(
                                    childPath,
                                    { dir: Path -> notifyCreateEvent(true, dir, count, rootPath); true },
                                    { file: Path -> notifyCreateEvent(false, file, count, rootPath); true },
                                    maxDepth
                                )
                            }
                        }
                        notifyCreateEvent(isDirectory, childPath, count, rootPath)
                    } else if (kind === StandardWatchEventKinds.ENTRY_MODIFY) {
                        val isDirectory = directories.contains(childPath)
                        if (fileHasher == null) {
                            onEvent(DirectoryChangeEvent.EventType.MODIFY, isDirectory, childPath, count, rootPath)
                        } else {
                            /*
                   * Note that existingHash may be null due to the file being created before we
                   * start listening It's important we don't discard the event in this case
                   */
                            val existingHash = pathHashes[childPath]

                            /*
                   * newHash can be null when using File#delete() on windows - it generates MODIFY
                   * and DELETE in succession. In this case the MODIFY event can be safely ignored
                   */
                            val newHash = PathUtils.hash(fileHasher, childPath)
                            if (newHash != null && newHash != existingHash) {
                                pathHashes[childPath] = newHash
                                onEvent(DirectoryChangeEvent.EventType.MODIFY, isDirectory, childPath, count, rootPath)
                            } else if (newHash == null) {
                                logger.debug(
                                    "Failed to hash modified file [{}]. It may have been deleted.",
                                    childPath
                                )
                            }
                        }
                    } else if (kind === StandardWatchEventKinds.ENTRY_DELETE) {
                        if (fileHasher == null) {
                            val isDirectory = directories.remove(childPath)
                            // hashing is disabled, so just notify on the path we got the event for
                            onEvent(DirectoryChangeEvent.EventType.DELETE, isDirectory, childPath, count, rootPath)
                        } else {
                            // hashing is enabled, so delete the hashes
                            val subtreePaths = PathUtils.subMap(pathHashes, childPath).keys
                            for (path in subtreePaths) {
                                val isDirectory = directories.remove(path)
                                onEvent(DirectoryChangeEvent.EventType.DELETE, isDirectory, path, count, rootPath)
                            }
                            // this will remove from the original map
                            subtreePaths.clear()
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("DirectoryWatcher got an exception while watching!", e)
                    listener.onException(e)
                }
            }
            val valid = key.reset()
            if (!valid) {
                logger.debug("WatchKey for [{}] no longer valid; removing.", key.watchable())
                // remove the key from the keyRoots
                val registeredPath = keyRoots.remove(key)

                // Also remove from the registeredPathToRootPath maps
                registeredPathToRootPath.remove(registeredPath)

                // if there are no more keys left to watch, we can break out
                if (keyRoots.isEmpty()) {
                    logger.debug("No more directories left to watch; terminating watcher.")
                    break
                }
            }
        }
        try {
            close()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun onEvent(
        eventType: DirectoryChangeEvent.EventType,
        isDirectory: Boolean,
        childPath: Path?,
        count: Int,
        rootPath: Path?
    ) {
        logger.debug("-> {} [{}] (isDirectory: {})", eventType, childPath, isDirectory)
        val hash = pathHashes[childPath]
        listener.onEvent(
            DirectoryChangeEvent(eventType, isDirectory, childPath, hash, count, rootPath)
        )
    }

    fun close() {
        watchService.close()
        isClosed = true
    }

    private fun registerAll(start: Path?, context: Path?) {
        if (false != fileTreeSupported) {
            // Try using FILE_TREE modifier since we aren't certain that it's unsupported
            try {
                register(start, true, context)
                // Assume FILE_TREE is supported
                fileTreeSupported = true
            } catch (e: UnsupportedOperationException) {
                // UnsupportedOperationException should only happen if FILE_TREE is unsupported
                logger.debug("Assuming ExtendedWatchEventModifier.FILE_TREE is not supported", e)
                fileTreeSupported = false
                // If we failed to use the FILE_TREE modifier, try again without
                registerAll(start, context)
            }
        } else {
            // Since FILE_TREE is unsupported, register root directory and sub-directories
            PathUtils.recursiveVisitFiles(
                start,
                { register(it, false, context); true },
                { true },
                maxDepth
            )
        }
    }

    // Internal method to be used by registerAll
    private fun register(directory: Path?, useFileTreeModifier: Boolean, context: Path?) {
        logger.debug("Registering [{}].", directory)
        val watchable = if (isMac) WatchablePath(directory) else directory!!
        val modifiers = if (useFileTreeModifier) arrayOf<WatchEvent.Modifier>(ExtendedWatchEventModifier.FILE_TREE) else arrayOf()
        val kinds = arrayOf<WatchEvent.Kind<*>>(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
        val watchKey = watchable.register(watchService, kinds, *modifiers)
        keyRoots[watchKey] = directory
        registeredPathToRootPath[directory] = context
    }

    private fun notifyCreateEvent(isDirectory: Boolean, path: Path?, count: Int, rootPath: Path?) {
        if (fileHasher != null) {
            val newHash = PathUtils.hash(fileHasher, path)
            if (newHash == null) {
                // Hashing could fail for locked files on Windows.
                // Skip notification only if we confirm the file does not exist.
                if (Files.notExists(path)) {
                    logger.debug("Failed to hash created file [{}]. It may have been deleted.", path)
                    // Skip notifying the event.
                    return
                } else {
                    // Just warn here and continue to notify the event.
                    logger.debug("Failed to hash created file [{}]. It may be locked.", path)
                }
            } else if (pathHashes.put(path, newHash) != null) {
                // Skip notifying the event if we've already seen the path.
                logger.debug("Skipping create event for path [{}]. Path already hashed.", path)
                return
            }
        }
        if (isDirectory) {
            directories.add(path)
        }
        onEvent(DirectoryChangeEvent.EventType.CREATE, isDirectory, path, count, rootPath)
    }
}
