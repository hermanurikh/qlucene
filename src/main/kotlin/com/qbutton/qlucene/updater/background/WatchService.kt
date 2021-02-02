package com.qbutton.qlucene.updater.background

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FileValidator
import com.qbutton.qlucene.common.IndexCanceller
import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.updater.FileUpdaterFacade
import com.qbutton.qlucene.updater.background.io.methvin.watcher.DirectoryWatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Phaser
import javax.annotation.PreDestroy

@Component
class WatchService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${directory.index.max-depth}")
    private val maxDepth: Int,
    @Value("\${file.polling-interval}")
    private val filePollingInterval: Long,
    private val fileValidator: FileValidator,
    private val indexCanceller: IndexCanceller,
    private val fileIdConverter: FileIdConverter,
    private val fileUpdaterFacade: FileUpdaterFacade,
    private val qLuceneExecutorService: ExecutorService
) : Resettable {

    private val logger = LoggerFactory.getLogger(WatchService::class.java)
    private val directoryWatchers = Collections.newSetFromMap(ConcurrentHashMap<DirectoryWatcher, Boolean>())

    fun attachWatcherToFile(path: Path) {
        val watcher = SingleFileWatcher(path, applicationEventPublisher, filePollingInterval)
        qLuceneExecutorService.submit(watcher)
    }

    fun attachWatcherToRootDirAndIndex(path: Path): Pair<DirectoryWatcher, Set<String>> {
        val addedFileIds = HashSet<String>()
        val phaser = Phaser(1)
        val watcher = createWatcher(path, addedFileIds, phaser)
        directoryWatchers.add(watcher)
        watcher.watchAsync(qLuceneExecutorService)

        // wait until all file indexing tasks which we have submitted in createWatcher method finish executing
        phaser.arriveAndAwaitAdvance()
        return Pair(watcher, addedFileIds)
    }

    /**
     * Creates the watcher from io.methwin library. It traverses file system on initialization,
     * so we index at the same time as attaching watchers.
     */
    private fun createWatcher(path: Path, addedFileIds: HashSet<String>, phaser: Phaser) = DirectoryWatcher(
        path = path,
        isTraversalCancelledForId = indexCanceller::isCancelled,
        toFileIdAction = fileIdConverter::toId,
        listener = { applicationEventPublisher.publishEvent(it) },
        registeredFileIds = addedFileIds,
        indexFileAction = {
            phaser.register()
            qLuceneExecutorService.submit {
                if (fileValidator.isValid(it)) {
                    fileUpdaterFacade.update(fileIdConverter.toId(it.toAbsolutePath()))
                }
                phaser.arriveAndDeregister()
            }
        },
        maxDepth = maxDepth,
    )

    override fun resetState() {
        directoryWatchers.forEach { it.close() }
        directoryWatchers.clear()
    }

    @PreDestroy
    fun stopWatchers() {
        logger.info("stopping watchers")
        directoryWatchers.forEach { it.close() }
    }
}
