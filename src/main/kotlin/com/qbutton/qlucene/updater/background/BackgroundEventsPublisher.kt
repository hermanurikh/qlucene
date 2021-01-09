package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.DirectoryChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
class BackgroundEventsPublisher @Autowired constructor(
    private val jdkWatchService: WatchService,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Resettable {

    private val keyMap = ConcurrentHashMap<WatchKey, Path>()

    // if there is an entry in dirToFilteredFiles and set is not empty, we should be interested only in those files which are in the set
    private val dirToFilteredFiles = ConcurrentHashMap<String, MutableSet<String>>()

    // we are fine to process updates in single thread since it will be just adding events to spring event bus
    private val executorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var processEvents = true

    private val logger = LoggerFactory.getLogger(BackgroundEventsPublisher::class.java)!!

    fun attachWatcher(path: String) {
        val dir = Paths.get(path)
        val key = dir.register(jdkWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keyMap[key] = dir
    }

    fun updateFilteredFiles(path: String, filteredFile: String) {
        val filteredFiles = dirToFilteredFiles.computeIfAbsent(path) { HashSet() }
        filteredFiles.add(filteredFile)
    }

    fun clearFilteredFiles(path: String) {
        dirToFilteredFiles.remove(path)
    }

    /**
     * Listens to events reported by Java nio API and publishes them to spring events queue to later be properly processed.
     * The reason of this is to decouple filesystem events from application events, so that filesystem events listener was super-lightweight.
     */
    @PostConstruct
    fun processEvents() {
        executorService.submit {
            logger.info("started waiting for file changed events")
            while (processEvents) {

                val key: WatchKey = jdkWatchService.take()
                val dir = keyMap[key]
                if (dir == null) {
                    logger.error("Dir not found for key $key")
                    continue
                }
                @Suppress("UNCHECKED_CAST")
                val event = DirectoryChangedEvent(dir, key.pollEvents() as List<WatchEvent<Path>>, dirToFilteredFiles[dir.toString()])
                applicationEventPublisher.publishEvent(event)

                // reset key and remove from set if directory no longer accessible
                val valid = key.reset()
                if (!valid) {
                    keyMap.remove(key)

                    // all directories are inaccessible
                    if (keyMap.isEmpty()) {
                        break
                    }
                }
            }
        }
    }

    @PreDestroy
    fun close() {
        logger.info("Shutting down watch service")
        processEvents = false
        keyMap.forEach { (k, _) -> k.cancel() }
        executorService.shutdown()
        jdkWatchService.close()
    }

    override fun resetState() {
        dirToFilteredFiles.clear()
        keyMap.forEach { (k, _) -> k.cancel() }
        keyMap.clear()
    }
}
