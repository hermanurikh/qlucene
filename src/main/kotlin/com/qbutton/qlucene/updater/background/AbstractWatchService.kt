package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.FileChangedEvent
import com.qbutton.qlucene.dto.RegistrationResult
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * A base class to attach watchers. We need 2 different watchers for directories and files modification - because
 * a file and directory can have the same name.
 */
abstract class AbstractWatchService constructor(
    private val jdkWatchService: WatchService,
    private val fileIdConverter: FileIdConverter,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val keyMap = HashMap<WatchKey, Path>()
    private val monitoredDirectories = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    // we are fine to process updates in single thread since it will be just adding events to spring event bus
    private val executorService = Executors.newSingleThreadExecutor()
    @Volatile private var processEvents = true

    private val logger = LoggerFactory.getLogger(AbstractWatchService::class.java)!!

    fun tryMonitor(path: String): Boolean {
        val dirId = fileIdConverter.toId(path)
        return monitoredDirectories.add(dirId)
    }

    abstract fun register(path: String): RegistrationResult

    fun attachWatcher(path: String) {
        val dir = Paths.get(path)
        val key = dir.register(jdkWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW)
        keyMap[key] = dir
    }

    @PostConstruct
    fun processEvents() {
        executorService.submit {
            while (processEvents) {
                logger.info("waiting")
                val key: WatchKey = jdkWatchService.take()
                val dir = keyMap[key]
                if (dir == null) {
                    logger.error("Dir not found for key $key")
                    continue
                }
                @Suppress("UNCHECKED_CAST")
                val event = FileChangedEvent(dir, key.pollEvents() as List<WatchEvent<Path>>)
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
        logger.info("Shutting down watch service $this")
        processEvents = false
        executorService.shutdown()
        jdkWatchService.close()
    }
}
