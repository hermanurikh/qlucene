package com.qbutton.qlucene.updater.tokenizer

import com.qbutton.qlucene.dto.Sentence
import org.springframework.stereotype.Component
import java.text.BreakIterator

@Component
class SentenceTokenizer : BreakIteratorTokenizer() {
    // splits into sentences. See tests for examples
    override fun getBreakIterator() = BreakIterator.getSentenceInstance()!!

    override fun toTerm(rawToken: String) = Sentence(rawToken)

    override fun getProducedTermClass() = Sentence::class
}
