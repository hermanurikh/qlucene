package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.UpdateIndexInput
import com.qbutton.qlucene.fileaccess.FileReader
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.updater.tokenizer.Tokenizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpdaterFacade @Autowired constructor(
    private val diffCalculator: DiffCalculator,
    private val tokenizers: List<Tokenizer>,
    private val indices: List<Index>,
    private val fileReader: FileReader
) {
    // todo dont forget tests with empty files
    fun update(fileId: String) {
        // todo we may need a lock on file here not to process 2 files simultanously
        val oldFile = fileReader.getLastIndexedContents(fileId)
        val newFile = fileReader.getContents(fileId)
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
