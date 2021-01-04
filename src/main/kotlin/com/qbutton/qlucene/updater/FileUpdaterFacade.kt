package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.UpdateIndexInput
import com.qbutton.qlucene.fileaccess.FileStorageFacade
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.updater.tokenizer.Tokenizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Component
class FileUpdaterFacade @Autowired constructor(
    private val diffCalculator: DiffCalculator,
    private val tokenizers: List<Tokenizer>,
    private val indices: List<Index>,
    private val fileStorageFacade: FileStorageFacade
) {
    private val locks = ConcurrentHashMap<String, Lock>()

    // todo dont forget tests with empty files
    fun update(fileId: String) {
        // todo how about a quick check if hashes are same and no need to check?
        val lock = locks.computeIfAbsent(fileId) { ReentrantLock() }
        val oldFile: String
        val newFile: String
        try {
            // update may be called for same file from different threads simultaneously. Following operations are not atomic, we need to lock while executing
            lock.lock()
            oldFile = fileStorageFacade.getLastIndexedContents(fileId)
            newFile = fileStorageFacade.readRawTextFromFileSystem(fileId)
            fileStorageFacade.updateIndexedContents(fileId, newFile)
        } finally {
            /* no need to hold lock any more, we've updated the cached contents and have the diff,
             now we just need to propagate diff to index which is eventually consistent
             */
            lock.unlock()
        }
        // loading files up to 10MB (which was a top limit in requirements) and comparing the tokens looks almost instant (< 1 second)
        for (tokenizer in tokenizers) {
            val oldTokens = tokenizer.tokenize(oldFile)
            val newTokens = tokenizer.tokenize(newFile)
            val diff = diffCalculator.getDiff(oldTokens, newTokens)
            val filteredIndices = indices.filter { it.canExecute(tokenizer.getProducedTermClass()) }

            diff.parallelStream()
                .map { UpdateIndexInput(tokenizer.toTerm(it.token), it.operation, fileId, it.count) }
                .forEach { indexUpdateInfo -> filteredIndices.forEach { it.update(indexUpdateInfo) } }
        }
    }
}
