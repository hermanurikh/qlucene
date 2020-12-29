package com.qbutton.qlucene.updater

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpdaterFacade @Autowired constructor(
        private val diffCalculator: DiffCalculator
) {
    fun update(oldFile: String, newFile: String) {
        val diff = diffCalculator.getDiff(oldFile, newFile)

        //for each tokenizer -> get tokens, then update index
    }
}