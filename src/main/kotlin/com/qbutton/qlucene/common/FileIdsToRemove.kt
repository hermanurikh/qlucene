package com.qbutton.qlucene.common

import com.qbutton.qlucene.index.Index
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * This component is responsible for managing "dirty" fileIds which have been added to index, but require removal
 * due to operation cancellation.
 */
@Component
class FileIdsToRemove(
    private val indices: List<Index>
) {

    private val logger = LoggerFactory.getLogger(FileIdsToRemove::class.java)
    private val fileIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun addAll(newFileIds: Set<String>) {
        fileIds.addAll(newFileIds)
    }

    fun removeAll(newFileIds: Set<String>) {
        fileIds.removeAll(newFileIds)
    }

    fun contains(fileId: String) = fileIds.contains(fileId)

    @Scheduled(fixedDelay = 10000)
    fun cleanUpFileIds() {
        if (fileIds.isNotEmpty()) {
            logger.info("cleaning up file ids")
            val it = fileIds.iterator()
            while (it.hasNext()) {
                val fileId = it.next()
                indices.forEach { index -> index.remove(fileId) }
                it.remove()
            }
        }
    }
}
