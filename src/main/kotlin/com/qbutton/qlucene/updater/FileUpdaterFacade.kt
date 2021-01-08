package com.qbutton.qlucene.updater

import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.UpdateIndexInput
import com.qbutton.qlucene.fileaccess.FileStorageFacade
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.updater.tokenizer.Tokenizer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A facade to perform file update operation:
 *      1. get last indexed contents
 *      2. get current contents
 *      3. calculate difference and feed it to indices
 *      4. update last indexed contents
 */
@Component
class FileUpdaterFacade @Autowired constructor(
    private val diffCalculator: DiffCalculator,
    private val tokenizers: List<Tokenizer>,
    private val indices: List<Index>,
    private val fileStorageFacade: FileStorageFacade
) : Resettable {
    private val locks = ConcurrentHashMap<String, Lock>()
    private val logger = LoggerFactory.getLogger(FileUpdaterFacade::class.java)

    fun update(fileId: String) {
        logger.info("updating file $fileId")

        val lock = locks.computeIfAbsent(fileId) { ReentrantLock() }
        val oldFile: String
        val newFile: String
        try {
            // update may be called for same file from different threads simultaneously. Following operations are not atomic, we need to lock while executing
            lock.lock()
            oldFile = fileStorageFacade.getLastIndexedContents(fileId)
            newFile = fileStorageFacade.readRawTextFromFileSystem(fileId)

            if (hashesAreEqual(oldFile, newFile)) {
                logger.info("file $fileId contents look identical, skipping updates")
                return
            }

            fileStorageFacade.updateIndexedContents(fileId, newFile)
        } finally {
            /* no need to hold lock any more, we've updated the cached contents and have the diff,
             now we just need to propagate diff to index which is eventually consistent
             */
            lock.unlock()
        }
        // loading files up to 10MB (which was a top limit in requirements) and comparing the tokens looks almost instant (< 1 second)
        for (tokenizer in tokenizers) {
            logger.info("tokenizing with $tokenizer")
            val oldTokens = tokenizer.tokenize(oldFile)
            val newTokens = tokenizer.tokenize(newFile)
            val diff = diffCalculator.getDiff(oldTokens, newTokens)
            logger.info("diff calculated for file $fileId")
            val filteredIndices = indices.filter { it.canExecute(tokenizer.getProducedTermClass()) }

            diff.parallelStream()
                .map { UpdateIndexInput(it.token, it.operation, fileId, it.count) }
                .forEach { indexUpdateInfo -> filteredIndices.forEach { it.update(indexUpdateInfo) } }
            logger.info("finished updating index for tokenizer $tokenizer")
        }
    }

    private fun hashesAreEqual(oldFile: String, newFile: String): Boolean {
        val oldFileHash = DigestUtils.md5Digest(oldFile.toByteArray())
        val newFileHash = DigestUtils.md5Digest(newFile.toByteArray())

        return oldFileHash.contentEquals(newFileHash)
    }

    override fun resetState() {
        locks.clear()
    }
}
