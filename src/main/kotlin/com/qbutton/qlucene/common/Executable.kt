package com.qbutton.qlucene.common

import com.qbutton.qlucene.dto.Term
import kotlin.reflect.KClass

/**
 * An interface to be able to switch on/off implementing classes based on some condition.
 */
interface Executable {
    fun canExecute(term: Term) = canExecute(term::class)

    fun canExecute(clazz: KClass<out Term>) = true
}
