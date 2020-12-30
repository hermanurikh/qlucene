package com.qbutton.qlucene.updater.tokenizer

import org.springframework.stereotype.Component

@Component
class WordTokenizer : Tokenizer() {
    // splits into words according to the best understanding of word is, see tests for more
    // basically, split on all punctuation excluding '_' and '-' or whitespaces
    override fun tokenize(rawText: String) = rawText.split("[[\\p{Punct}&&[^_-]]\\s]+".toRegex()).filter { it.isNotBlank() }
}
