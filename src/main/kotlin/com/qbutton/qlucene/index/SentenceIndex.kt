package com.qbutton.qlucene.index

import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Term
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SentenceIndex @Autowired constructor(
        @Value("\${sentence.index.enabled}")
        private val isSentenceIndexEnabled : Boolean
) : Index() {

    override fun canExecute(term: Term) = term is Sentence && isSentenceIndexEnabled
}