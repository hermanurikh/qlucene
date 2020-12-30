package com.qbutton.qlucene.updater.tokenizer

import org.springframework.stereotype.Component
import java.text.BreakIterator

@Component
class SentenceTokenizer : BreakIteratorTokenizer() {
    // splits into sentences. See tests for examples
    override fun getBreakIterator() = BreakIterator.getSentenceInstance()!!
}
