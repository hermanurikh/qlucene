package com.qbutton.qlucene.updater.tokenizer

import com.qbutton.qlucene.dto.Term
import kotlin.reflect.KClass

abstract class Tokenizer {
    /**
     * Splits raw text into a list of tokens.
     */
    abstract fun tokenize(rawText: String): Map<Term, Int>

    abstract fun getProducedTermClass(): KClass<out Term>
}
