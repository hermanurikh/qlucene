package com.qbutton.qlucene.updater

import com.qbutton.qlucene.updater.tokenizer.Tokenizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpdaterFacade @Autowired constructor(
    private val diffCalculator: DiffCalculator,
    private val tokenizers: List<Tokenizer>
) {
    fun update(oldFile: String, newFile: String) {
        val diff = diffCalculator.getDiff(oldFile, newFile)
        // loading files up to 10MB (which was a top limit in requirements) and comparing the tokens looks almost instant (< 1 second)

        /*
        for each tokenizer ->
            get tokens old and new
            find diff in tokens
            feed diff to index
         */
    }
}
