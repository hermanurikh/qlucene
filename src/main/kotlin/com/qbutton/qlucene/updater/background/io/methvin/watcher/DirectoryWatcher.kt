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
import java.util.concurrent.ForkJoinPool

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
This is a copy of io.methvin.watcher.DirectoryWatcher, with a small patch allowing the library to break in the middle
of traversing file path.
 */

class DirectoryWatcher(
    paths: List<Path?>,
    listener: DirectoryChangeListener,
    watchService: WatchService?,
    fileHasher: FileHasher?,
    logger: Logger?
) {
    /**
     * A builder for a [DirectoryWatcher]. Use `DirectoryWatcher.builder()` to get a new
     * instance.
     */
    class Builder constructor() {
        private var paths = emptyList<Path>()
        private var listener = DirectoryChangeListener { _: DirectoryChangeEvent? -> }
        private var logger: Logger? = null
        private var fileHasher = FileHasher.DEFAULT_FILE_HASHER
        private var watchService: WatchService? = null

        /** Set multiple paths to watch.  */
        fun paths(paths: List<Path>): Builder {
            this.paths = paths
            return this
        }

        /** Set a single path to watch.  */
        fun path(path: Path): Builder {
            return paths(listOf(path))
        }

        /** Set a listener that will be called when a directory change event occurs.  */
        fun listener(listener: DirectoryChangeListener): Builder {
            this.listener = listener
            return this
        }

        /**
         * Set a [WatchService] implementation that will be used by the watcher.
         *
         *
         * By default, this detects your OS and either uses the native JVM watcher or the macOS
         * watcher.
         */
        fun watchService(watchService: WatchService?): Builder {
            this.watchService = watchService
            return this
        }

        /**
         * Set a logger to be used by the watcher. This defaults to `LoggerFactory.getLogger(DirectoryWatcher.class)`
         */
        fun logger(logger: Logger?): Builder {
            this.logger = logger
            return this
        }

        /**
         * Defines whether file hashing should be used to catch duplicate events. Defaults to `true`.
         */
        fun fileHashing(enabled: Boolean): Builder {
            fileHasher = if (enabled) FileHasher.DEFAULT_FILE_HASHER else null
            return this
        }

        /**
         * Defines the file hasher to be used by the watcher.
         *
         *
         * Note: will implicitly enable file hashing. Setting to null is equivalent to `fileHashing(false)`
         */
        fun fileHasher(fileHasher: FileHasher?): Builder {
            this.fileHasher = fileHasher
            return this
        }

        @Throws(IOException::class)
        fun build(): DirectoryWatcher {
            if (watchService == null) {
                osDefaultWatchService()
            }
            if (logger == null) {
                staticLogger()
            }
            return DirectoryWatcher(paths, listener, watchService, fileHasher, logger)
        }

        @Throws(IOException::class)
        private fun osDefaultWatchService(): Builder {
            val isMac = System.getProperty("os.name").toLowerCase().contains("mac")
            return if (isMac) {
                watchService(
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
                )
            } else {
                watchService(FileSystems.getDefault().newWatchService())
            }
        }

        private fun staticLogger(): Builder {
            return logger(LoggerFactory.getLogger(DirectoryWatcher::class.java))
        }
    }

    private val logger: Logger?
    private val watchService: WatchService?
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
    /**
     * Asynchronously watch the directories.
     *
     * @param executor the executor to use to watch asynchronously
     */
    /**
     * Asynchronously watch the directories using `ForkJoinPool.commonPool()` as the executor
     */
    @JvmOverloads
    fun watchAsync(executor: Executor? = ForkJoinPool.commonPool()): CompletableFuture<Void?> {
        return CompletableFuture.supplyAsync(
            {
                watch()
                null
            },
            executor
        )
    }

    fun pathHashes(): Map<Path?, FileHash?> {
        return pathHashes.toMap()
    }

    /**
     * Watch the directories. Block until either the listener stops watching or the DirectoryWatcher
     * is closed.
     */
    fun watch() {
        while (listener.isWatching) {
            // wait for key to be signalled
            var key: WatchKey
            key = try {
                watchService!!.take()
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
                            if (java.lang.Boolean.TRUE != fileTreeSupported) {
                                registerAll(childPath, rootPath)
                            }
                            /*
                   * Our custom Mac service sends subdirectory changes but the Windows/Linux do
                   * not. Walk the file tree to make sure we send create events for any files that
                   * were created.
                   */if (!isMac) {
                                PathUtils.recursiveVisitFiles(
                                    childPath,
                                    { dir: Path? -> notifyCreateEvent(true, dir, count, rootPath) },
                                    { file: Path? -> notifyCreateEvent(false, file, count, rootPath) }
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
                                logger!!.debug(
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
                    logger!!.debug("DirectoryWatcher got an exception while watching!", e)
                    listener.onException(e)
                }
            }
            val valid = key.reset()
            if (!valid) {
                logger!!.debug("WatchKey for [{}] no longer valid; removing.", key.watchable())
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

    @Throws(IOException::class)
    private fun onEvent(
        eventType: DirectoryChangeEvent.EventType,
        isDirectory: Boolean,
        childPath: Path?,
        count: Int,
        rootPath: Path?
    ) {
        logger!!.debug("-> {} [{}] (isDirectory: {})", eventType, childPath, isDirectory)
        val hash = pathHashes[childPath]
        listener.onEvent(
            DirectoryChangeEvent(eventType, isDirectory, childPath, hash, count, rootPath)
        )
    }

    @Throws(IOException::class)
    fun close() {
        watchService!!.close()
        isClosed = true
    }

    @Throws(IOException::class)
    private fun registerAll(start: Path?, context: Path?) {
        if (java.lang.Boolean.FALSE != fileTreeSupported) {
            // Try using FILE_TREE modifier since we aren't certain that it's unsupported
            try {
                register(start, true, context)
                // Assume FILE_TREE is supported
                fileTreeSupported = true
            } catch (e: UnsupportedOperationException) {
                // UnsupportedOperationException should only happen if FILE_TREE is unsupported
                logger!!.debug("Assuming ExtendedWatchEventModifier.FILE_TREE is not supported", e)
                fileTreeSupported = false
                // If we failed to use the FILE_TREE modifier, try again without
                registerAll(start, context)
            }
        } else {
            // Since FILE_TREE is unsupported, register root directory and sub-directories
            PathUtils.recursiveVisitFiles(start, { dir: Path? -> register(dir, false, context) }, { })
        }
    }

    // Internal method to be used by registerAll
    @Throws(IOException::class)
    private fun register(directory: Path?, useFileTreeModifier: Boolean, context: Path?) {
        logger!!.debug("Registering [{}].", directory)
        val watchable = if (isMac) WatchablePath(directory) else directory!!
        val modifiers = if (useFileTreeModifier) arrayOf<WatchEvent.Modifier>(ExtendedWatchEventModifier.FILE_TREE) else arrayOf()
        val kinds = arrayOf<WatchEvent.Kind<*>>(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
        val watchKey = watchable.register(watchService, kinds, *modifiers)
        keyRoots[watchKey] = directory
        registeredPathToRootPath[directory] = context
    }

    @Throws(IOException::class)
    private fun notifyCreateEvent(isDirectory: Boolean, path: Path?, count: Int, rootPath: Path?) {
        if (fileHasher != null) {
            val newHash = PathUtils.hash(fileHasher, path)
            if (newHash == null) {
                // Hashing could fail for locked files on Windows.
                // Skip notification only if we confirm the file does not exist.
                if (Files.notExists(path)) {
                    logger!!.debug("Failed to hash created file [{}]. It may have been deleted.", path)
                    // Skip notifying the event.
                    return
                } else {
                    // Just warn here and continue to notify the event.
                    logger!!.debug("Failed to hash created file [{}]. It may be locked.", path)
                }
            } else if (pathHashes.put(path, newHash) != null) {
                // Skip notifying the event if we've already seen the path.
                logger!!.debug("Skipping create event for path [{}]. Path already hashed.", path)
                return
            }
        }
        if (isDirectory) {
            directories.add(path)
        }
        onEvent(DirectoryChangeEvent.EventType.CREATE, isDirectory, path, count, rootPath)
    }

    companion object {
        /** Get a new builder for a [DirectoryWatcher].  */
        fun builder(): Builder {
            return Builder()
        }
    }

    init {
        registeredPathToRootPath = HashMap()
        this.listener = listener
        this.watchService = watchService
        isMac = watchService is MacOSXListeningWatchService
        pathHashes = ConcurrentSkipListMap()
        directories = Collections.newSetFromMap(ConcurrentHashMap())
        keyRoots = ConcurrentHashMap()
        this.fileHasher = fileHasher
        this.logger = logger
        PathUtils.initWatcherState(paths, fileHasher, pathHashes, directories)
        for (path in paths) {
            registerAll(path, path)
        }
    }
}
