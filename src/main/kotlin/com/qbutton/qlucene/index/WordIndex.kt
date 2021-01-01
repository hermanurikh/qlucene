package com.qbutton.qlucene.index

import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.Word
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Component
class WordIndex @Autowired constructor(
    @Value("\${word.index.enabled}")
    private val isWordIndexEnabled: Boolean
) : Index() {

    override fun canExecute(clazz: KClass<out Term>) = clazz.isSubclassOf(Word::class) && isWordIndexEnabled
}
