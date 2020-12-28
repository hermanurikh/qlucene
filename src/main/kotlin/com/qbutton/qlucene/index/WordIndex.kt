package com.qbutton.qlucene.index

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.Word
import org.springframework.stereotype.Component

@Component
class WordIndex : Index {
    override fun find(term: Term): List<DocumentSearchResult> {
        TODO("Not yet implemented")
    }

    override fun accepts(term: Term) = term is Word
}