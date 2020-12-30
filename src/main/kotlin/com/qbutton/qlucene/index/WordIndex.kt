package com.qbutton.qlucene.index

import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.Word
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WordIndex @Autowired constructor(
    @Value("\${word.index.enabled}")
    private val isWordIndexEnabled: Boolean
) : Index() {

    override fun canExecute(term: Term) = term is Word && isWordIndexEnabled
}
