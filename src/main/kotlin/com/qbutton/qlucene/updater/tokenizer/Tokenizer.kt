package com.qbutton.qlucene.updater.tokenizer

abstract class Tokenizer {
    abstract fun tokenize(rawText: String): List<String>
}
