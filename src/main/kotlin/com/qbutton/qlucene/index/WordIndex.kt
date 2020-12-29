package com.qbutton.qlucene.index

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.Word
import com.qbutton.qlucene.updater.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WordIndex @Autowired constructor(
        //todo why does it work? Refactor properties to another bean
        @Value("\${word.index.enabled}")
        private val isWordIndexEnabled : Boolean
) : Index {
    override fun find(term: Term): Set<DocumentSearchResult> {
        TODO("Not yet implemented")
    }

    override fun update(term: Term, operation: Operation, fileId: String) {
        TODO("Not yet implemented")
    }

    override fun canExecute(term: Term) = term is Word && isWordIndexEnabled
}