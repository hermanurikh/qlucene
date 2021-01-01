package com.qbutton.qlucene.updater.tokenizer

import com.qbutton.qlucene.dto.Word
import org.springframework.stereotype.Component
import java.text.BreakIterator

@Component
class WordTokenizer : BreakIteratorTokenizer() {
    // splits into words according to BreakIterator's understanding of word is, see tests for more
    override fun getBreakIterator() = BreakIterator.getWordInstance()!!

    override fun toTerm(rawToken: String) = Word(rawToken)

    override fun getProducedTermClass() = Word::class
}
