package com.qbutton.qlucene.updater

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.Locker
import com.qbutton.qlucene.dto.UpdateIndexInput
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.updater.tokenizer.Tokenizer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

/**
 * A facade to perform file update operation:
 *      1. get last indexed contents
 *      2. get current contents
 *      3. calculate difference and feed it to indices
 *      4. update last indexed contents
 */
@Component
class FileUpdaterFacade @Autowired constructor(
    private val indices: List<Index>,
    private val locker: Locker,
    private val tokenizers: List<Tokenizer>,
    private val diffCalculator: DiffCalculator,
    private val fileIdConverter: FileIdConverter
) {
    private val logger = LoggerFactory.getLogger(FileUpdaterFacade::class.java)

    fun update(fileId: String) {
        logger.info("updating file $fileId")

        val newFile: String
        try {
            // update may be called for same file from different threads simultaneously. Following operations are not atomic, we need to lock while executing
            locker.lockId(fileId)
            newFile = readFromFileSystem(fileId)
            // loading files up to 10MB (which was a top limit in requirements) and comparing the tokens looks almost instant (< 1 second)
            for (tokenizer in tokenizers) {
                val filteredIndices = indices.filter { it.canExecute(tokenizer.getProducedTermClass()) }

                logger.info("tokenizing with $tokenizer")
                val newTokens = tokenizer.tokenize(newFile)

                filteredIndices.forEach { index ->
                    val oldTokens = index.findByDocId(fileId)
                    val diff = diffCalculator.getDiff(oldTokens, newTokens)
                    logger.info("diff calculated for file $fileId")
                    diff.parallelStream()
                        .map { UpdateIndexInput(it.token, it.operation, fileId, it.count) }
                        .forEach { index.update(it) }
                    index.updateReverseIndex(fileId, newTokens)
                }

                logger.info("finished updating indexes for tokenizer $tokenizer")
            }
        } finally {
            locker.unlockId(fileId)
        }
    }

    private fun readFromFileSystem(fileId: String): String {
        val fileName = fileIdConverter.toPath(fileId)
        val path = Paths.get(fileName)
        // empty contents is a valid result since we might have removed the file
        return if (Files.exists(path)) path.toFile().readText() else ""
    }
}
